/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it 
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of 
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more 
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.

 */
package eu.europa.ec.fisheries.uvms.mdr.message.consumer.bean;


import eu.europa.ec.fisheries.uvms.mdr.message.constants.MdrMessageConstants;
import eu.europa.ec.fisheries.uvms.mdr.message.event.GetFLUXFAReportMessageEvent;
import eu.europa.ec.fisheries.uvms.mdr.message.event.GetFLUXFMDRSyncMessageEvent;
import eu.europa.ec.fisheries.uvms.mdr.message.event.carrier.EventMessage;
import eu.europa.ec.fisheries.uvms.mdr.model.exception.MdrModelMarshallException;
import eu.europa.ec.fisheries.uvms.mdr.model.mapper.JAXBMarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import un.unece.uncefact.data.standard.communication.MdrModuleRequest;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;


@MessageDriven(mappedName = MdrMessageConstants.MDR_MESSAGE_IN_QUEUE, activationConfig = {
    @ActivationConfigProperty(propertyName = "messagingType", propertyValue = MdrMessageConstants.CONNECTION_TYPE),
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = MdrMessageConstants.DESTINATION_TYPE_QUEUE),
    @ActivationConfigProperty(propertyName = "destination", propertyValue = MdrMessageConstants.COMPONENT_MESSAGE_IN_QUEUE_NAME)
})
public class MessageConsumerBean implements MessageListener {

    final static Logger LOG = LoggerFactory.getLogger(MessageConsumerBean.class);

    @Inject
    @GetFLUXFAReportMessageEvent
    Event<EventMessage> getFLUXFAReportMessageEvent;
        
    @Inject
    @GetFLUXFMDRSyncMessageEvent
    Event<EventMessage> getFLUXFMDRSyncMessageEvent;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void onMessage(Message message) {

        TextMessage textMessage;
        try {
            textMessage = (TextMessage) message;
            LOG.info("Message received in activity");
            MdrModuleRequest request = JAXBMarshaller.unmarshallTextMessage(textMessage, MdrModuleRequest.class);
            LOG.info("Message unmarshalled successfully in activity");
            if(request==null){
                LOG.error("[ Request is null ]");
                return;
            }
            if (request.getMethod() == null) {
                LOG.error("[ Request method is null ]");
                return;
            }
            if(getFLUXFAReportMessageEvent==null){
                LOG.error("[ getFLUXFAReportMessageEvent is null ]");
                return;
            }

            switch (request.getMethod()) {

                case GET_FLUX_MDR_ENTITY : 
                	 getFLUXFMDRSyncMessageEvent.fire(new EventMessage(textMessage));
                     break;
                default:
                    LOG.error("[ Request method {} is not implemented ]", request.getMethod().name());
                   // errorEvent.fire(new EventMessage(textMessage, "[ Request method " + request.getMethod().name() + "  is not implemented ]"));
            }

        } catch (MdrModelMarshallException | NullPointerException | ClassCastException e) {
            LOG.error("[ Error when receiving message in activity: ] {}", e);
           // errorEvent.fire(new EventMessage(textMessage, "Error when receiving message in movement: " + e.getMessage()));
        }
    }

}
