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
 *  Observer class listening to events fired from MdrMessageConsumerBean (MDR Module).
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
			if(responseObject == null){
				log.error("The message received is not of type SetFLUXMDRSyncMessageResponse so it won't be attempted to save it! " +
						"Message content is as follows : "+extractMessageContent(message));
			}
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
		FLUXMDRReturnMessage respType = null;
		try {
			textMessage = extractMessageContent(message);
			SetFLUXMDRSyncMessageResponse mdrResp = JAXBMarshaller.unmarshallTextMessage(textMessage, SetFLUXMDRSyncMessageResponse.class);
			respType    = JAXBMarshaller.unmarshallTextMessage(mdrResp.getRequest(), FLUXMDRReturnMessage.class);
		} catch (MdrModelMarshallException e) {
			log.error(">> Error while attempting to Unmarshall Flux Response Object (XML MDR Entity) : \n",e.getMessage());
		}
		log.info("FluxMdrReturnMessage Unmarshalled successfully.. Going to save the data received! /n");
		return respType;
	}

	/**
	 * Extracts the message content from the EventMessage wrapper.
	 *
	 * @param  eventMessage
	 * @return textMessage
	 */
	private String extractMessageContent(EventMessage eventMessage) {
		String textMessage = null;
		try {
			textMessage = eventMessage.getJmsMessage().getText();
		} catch (JMSException e) {
			log.error("Error : The message is null or empty!");
		}
		return textMessage;
	}

}
