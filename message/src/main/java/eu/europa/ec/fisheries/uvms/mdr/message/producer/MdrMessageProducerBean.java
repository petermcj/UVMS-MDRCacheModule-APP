/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.

*/
package eu.europa.ec.fisheries.uvms.mdr.message.producer;

import eu.europa.ec.fisheries.uvms.exception.JmsMessageException;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.jms.Destination;
import javax.jms.Queue;

/**
 * Created by kovian on 02/12/2016.
 */
@Slf4j
@Stateless
public class MdrMessageProducerBean extends MdrAbstractProducer implements MdrGenericMessageProducer {

    @Resource(mappedName = MessageConstants.RULES_QUEUE)
    private Queue rulesQueue;

    @Resource(mappedName = MessageConstants.RULES_EVENT_QUEUE)
    private Queue rulesEventQueue;

    @Resource(mappedName = MessageConstants.ASSET_EVENT_QUEUE)
    private Queue assetsEventQueue;

    @Resource(mappedName = MessageConstants.MDR_QUEUE)
    private Queue mdrQueue;

    @Resource(mappedName = MessageConstants.MDR_EVENT_QUEUE)
    private Queue mdrEventQueue;

    /**
     * Sends a message to Exchange Queue.
     *
     * @param text (to be sent to the queue)
     * @return messageID
     */
    @Override
    public String sendRulesModuleMessage(String text) throws JmsMessageException {
        log.info("Sending Request to Rules module.");
        String messageID;
        try {
            messageID = sendModuleMessage(text, ModuleQueues.RULES_EVENT);
        } catch (JmsMessageException e) {
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
    public String sendModuleMessage(String text, ModuleQueues queue) throws JmsMessageException {
        String messageId;
        switch (queue) {

            case RULES:
                messageId = sendMessage(rulesQueue, text);
                break;
            case RULES_EVENT:
                messageId = sendMessage(rulesEventQueue, text);
                break;
            case MDR:
                messageId = sendMessage(mdrQueue, text);
                break;
            default:
                throw new JmsMessageException("Queue not defined or implemented");
        }
        return messageId;
    }

    @Override
    protected Destination getJmseToReplyTo() {
        return mdrEventQueue;
    }
}

