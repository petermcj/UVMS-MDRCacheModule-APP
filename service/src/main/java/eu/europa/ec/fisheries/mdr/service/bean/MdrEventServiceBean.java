/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it 
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of 
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more 
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.

 */
package eu.europa.ec.fisheries.mdr.service.bean;

import eu.europa.ec.fisheries.mdr.repository.MdrRepository;
import eu.europa.ec.fisheries.mdr.service.MdrEventService;
import eu.europa.ec.fisheries.uvms.mdr.message.event.MdrSyncMessageEvent;
import eu.europa.ec.fisheries.uvms.mdr.message.event.carrier.EventMessage;
import eu.europa.ec.fisheries.uvms.mdr.model.exception.MdrModelMarshallException;
import eu.europa.ec.fisheries.uvms.mdr.model.mapper.JAXBMarshaller;
import lombok.extern.slf4j.Slf4j;
import un.unece.uncefact.data.standard.mdr.communication.SetFLUXMDRSyncMessageResponse;
import un.unece.uncefact.data.standard.mdr.response.FLUXMDRReturnMessage;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.jms.JMSException;

/**
 *  Observer class listening to events fired from MessageConsumerBean (Activity).
 *  Specifically to MdrSyncMessageEvent event type.
 *  The message will contain the MDR Entity to be synchronised (As Flux XML Type at this moment).
 *  
 *  Using the MdrRepository the Entity in question will be stored in the Cache DB.
 *
 */
@Stateless
@Slf4j
public class MdrEventServiceBean implements MdrEventService {
	
	@EJB
	private MdrRepository mdrRepository;
	
	@Override
	public void recievedSyncMdrEntityMessage(@Observes @MdrSyncMessageEvent EventMessage message){
		log.info("-->> Recieved message from FLUX related to MDR Entity Synchronization.");
		// Extract message from EventMessage Object
		try {
			FLUXMDRReturnMessage responseObject = extractMdrFluxResponseFromEventMessage(message);
			mdrRepository.updateMdrEntity(responseObject);
		} catch (MdrModelMarshallException e) {
			log.error("MdrModelMarshallException while unmarshalling message from flux ",e);
		}
	}

	/**
	 * ResponseType from Flux Response.
	 * 
	 * @param message
	 * @return ResponseType
	 */
	private FLUXMDRReturnMessage extractMdrFluxResponseFromEventMessage(EventMessage message) throws MdrModelMarshallException {
		String textMessage;
		FLUXMDRReturnMessage respType;
		try {
			textMessage = message.getJmsMessage().getText();
			String adaptedMessage = adaptTextMessageToVersion16B(textMessage);
			SetFLUXMDRSyncMessageResponse mdrResp = JAXBMarshaller.unmarshallTextMessage(adaptedMessage, SetFLUXMDRSyncMessageResponse.class);
			respType    = JAXBMarshaller.unmarshallTextMessage(mdrResp.getRequest(), FLUXMDRReturnMessage.class);
		} catch (MdrModelMarshallException | JMSException e) {
			log.error("Error while attempting to Unmarshall Flux Response Object (XML MDR Entity) : \n",e);
			throw new MdrModelMarshallException("Error while attempting to Unmarshall Flux Response Object (XML MDR Entity)", e);
		}
		log.info("FluxMdrReturnMessage Unmarshalled successfully.. Going to save the data received! /n");
		return respType;
	}

	private String adaptTextMessageToVersion16B(String textMessage) {
		return textMessage.replace("UnqualifiedDataType:18", "UnqualifiedDataType:20")
				.replace("ReusableAggregateBusinessInformationEntity:18", "ReusableAggregateBusinessInformationEntity:20").
						replace("FLUXMDRQueryMessage:3","FLUXMDRQueryMessage:5")
				.replace("FLUXMDRReturnMessage:3", "FLUXMDRReturnMessage:5");
	}
}
