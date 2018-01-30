/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.

*/
package eu.europa.ec.fisheries.uvms.mdr.message.producer;

import eu.europa.ec.fisheries.uvms.commons.message.api.MessageException;
import eu.europa.ec.fisheries.uvms.mdr.message.constants.ModuleQueues;
import eu.europa.ec.fisheries.uvms.mdr.message.consumer.commonconsumers.MdrEventConsumer;
import eu.europa.ec.fisheries.uvms.mdr.message.producer.commonproducers.MdrQueueProducer;
import eu.europa.ec.fisheries.uvms.mdr.message.producer.commonproducers.RulesEventQueueProducer;
import eu.europa.ec.fisheries.uvms.mdr.message.producer.commonproducers.RulesQueueProducer;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.jms.Destination;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by kovian on 02/12/2016.
 */
@Slf4j
@Stateless
public class MdrMessageProducerBean implements IMdrMessageProducer {

    @EJB
    private MdrEventConsumer mdrEventQueueConsumer;

    @EJB
    private MdrQueueProducer mdrQueueProducer;

    @EJB
    private RulesEventQueueProducer rulesEventQueueProducer;

    @EJB
    private RulesQueueProducer rulesQueueProducer;

    /**
     * Sends a message to Rules Queue.
     *
     * @param text (to be sent to the queue)
     * @return messageID
     */
    @Override
    public String sendRulesModuleMessage(String text) throws MessageException {
        log.info("Sending Request to Rules module.");
        String messageID;
        try {
            messageID = rulesEventQueueProducer.sendModuleMessage(text, getMdrEventQueue());
        } catch (MessageException e) {
            log.error("Error sending message to Exchange Module.", e);
            throw e;
        }
        return messageID;
    }

    /**
     * Sends a message to a given Queue.
     *
     * @param text  (the message to be sent)
     * @param queue
     * @return JMSMessageID
     */
    @Override
    public String sendModuleMessage(String text, ModuleQueues queue) throws MessageException {
        String messageId;
        switch (queue) {

            case RULES:
                messageId = rulesQueueProducer.sendModuleMessage(text, getMdrEventQueue());
                break;
            case RULES_EVENT:
                messageId = rulesEventQueueProducer.sendModuleMessage(text, getMdrEventQueue());
                break;
            case MDR:
                messageId = mdrQueueProducer.sendModuleMessage(text, getMdrEventQueue());
                break;
            default:
                throw new MessageException("Queue not defined or implemented");
        }
        return messageId;
    }

    private Destination getMdrEventQueue(){
        return mdrEventQueueConsumer.getDestination();
    }
}

