/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it 
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of 
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more 
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.

 */
package eu.europa.ec.fisheries.mdr.service.bean;

import eu.europa.ec.fisheries.mdr.entities.codelists.baseentities.MasterDataRegistry;
import eu.europa.ec.fisheries.mdr.repository.MdrLuceneSearchRepository;
import eu.europa.ec.fisheries.mdr.repository.MdrRepository;
import eu.europa.ec.fisheries.mdr.service.MdrEventService;
import eu.europa.ec.fisheries.uvms.exception.ServiceException;
import eu.europa.ec.fisheries.uvms.mdr.message.event.GetMDRListMessageEvent;
import eu.europa.ec.fisheries.uvms.mdr.message.event.MdrSyncMessageEvent;
import eu.europa.ec.fisheries.uvms.mdr.message.event.carrier.EventMessage;
import eu.europa.ec.fisheries.uvms.mdr.message.producer.commonproducers.MdrQueueProducer;
import eu.europa.ec.fisheries.uvms.mdr.model.exception.MdrModelMarshallException;
import eu.europa.ec.fisheries.uvms.mdr.model.mapper.JAXBMarshaller;
import eu.europa.ec.fisheries.uvms.mdr.model.mapper.MdrModuleMapper;
import eu.europa.ec.fisheries.uvms.message.MessageException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import un.unece.uncefact.data.standard.mdr.communication.MdrGetCodeListRequest;
import un.unece.uncefact.data.standard.mdr.communication.SetFLUXMDRSyncMessageResponse;
import un.unece.uncefact.data.standard.mdr.communication.ValidationResultType;
import un.unece.uncefact.data.standard.mdr.response.FLUXMDRReturnMessage;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.jms.JMSException;
import java.util.List;

/**
 * Observer class listening to events fired from MdrMessageConsumerBean (MDR Module).
 * Specifically to MdrSyncMessageEvent event type.
 * The message will contain the MDR Entity to be synchronised (As Flux XML Type at this moment).
 * <p>
 * Using the MdrRepository the Entity in question will be stored in the Cache DB.
 */
@Stateless
@Slf4j
public class MdrEventServiceBean implements MdrEventService {

    public static final String STAR = "*";
    public static final String MDR_MODEL_MARSHALL_EXCEPTION = "MdrModelMarshallException while unmarshalling message from flux : ";
    public static final String ERROR_GET_LIST_FOR_THE_REQUESTED_CODE = "Error while trying to get list for the requested CodeList : ";
    public static final String ERROR_WHILE_SENDING_THE_RESPONSE_TO_MDR_QUEUE_OUT = "Error while sending the response to MdrQueue out : ";
    @EJB
    private MdrRepository mdrRepository;


    @EJB
    private MdrLuceneSearchRepository mdrSearchRepositroy;

    @EJB
    MdrQueueProducer mdrResponseQueueProducer;

    /**
     * This method saves the received codeList (from FLUX or MANUAL upload).
     *
     * @param message
     */
    @Override
    public void recievedSyncMdrEntityMessage(@Observes @MdrSyncMessageEvent EventMessage message) {
        log.info("-->> Recieved message from FLUX related to MDR Entity Synchronization.");
        // Extract message from EventMessage Object
        try {
            String messageStr = extractMessageRequestString(message);
            if (isAcnowledgeMessage(messageStr)) {
                log.info("ACKNOWLEDGE : Received Acnowledge Message. No data. Nothing is going to be persisted");
                return;
            }
            FLUXMDRReturnMessage responseObject = extractMdrFluxResponseFromEventMessage(messageStr);
            if (responseObject == null) {
                log.error("The message received is not of type SetFLUXMDRSyncMessageResponse so it won't be attempted to save it! " +
                        "Message content is as follows : " + extractMessageRequestString(message));
            }
            mdrRepository.updateMdrEntity(responseObject);
        } catch (MdrModelMarshallException e) {
            log.error("MdrModelMarshallException while unmarshalling message from flux ", e);
        }
    }

    /**
     * Thss method serves the request of getting a list (CodeList)
     *
     * @param message
     */
    @Override
    public void recievedGetMdrCodeListMessage(@Observes @GetMDRListMessageEvent EventMessage message) {
        log.info("-->> Recieved message from FLUX related to MDR Entity Synchronization.");
        // Extract request message
        MdrGetCodeListRequest requestObj;
        try {
            requestObj = extractMdrGetCodeListEventMessage(extractMessageRequestString(message));
            // Request is Not OK
            if (requestObj == null || StringUtils.isEmpty(requestObj.getAcronym())) {
                log.error("The message received is not of type MdrGetCodeListRequest so it won't be attempted to unmarshall it! " +
                        "Message content is as follows : " + extractMessageRequestString(message));
                sendErrorMessageToMdrQueue("Request object or Acronym for the request has not been specified!");
                return;
            }
            // Check query, Run query, Create response
            List<String> columnFilters = requestObj.getColumnsToFilters();
            String[] columnFiltersArr  = null;
            if(CollectionUtils.isNotEmpty(columnFilters)){
                columnFiltersArr = columnFilters.toArray(new String[columnFilters.size()]);
            }
            String filter = requestObj.getFilter();
            if(filter != null && !filter.equals(STAR)){
                filter = new StringBuilder(STAR).append(filter).append(STAR).toString();
            } else {
                filter = STAR;
            }
            List<? extends MasterDataRegistry> mdrList = mdrSearchRepositroy.findCodeListItemsByAcronymAndFilter(requestObj.getAcronym(),
                    0, 100, null, false, filter, columnFiltersArr);
            String mdrGetCodeListResponse = MdrModuleMapper.createFluxMdrGetCodeListResponse(mdrList, requestObj.getAcronym(),
                    ValidationResultType.OK, "Validation is OK.");
            mdrResponseQueueProducer.sendModuleMessage(mdrGetCodeListResponse, null);
        } catch (MdrModelMarshallException e) {
            sendErrorMessageToMdrQueue(MDR_MODEL_MARSHALL_EXCEPTION + e);
        } catch (ServiceException e) {
            sendErrorMessageToMdrQueue(ERROR_GET_LIST_FOR_THE_REQUESTED_CODE + e);
        } catch (MessageException e) {
            sendErrorMessageToMdrQueue(ERROR_WHILE_SENDING_THE_RESPONSE_TO_MDR_QUEUE_OUT + e);
        }
    }

    /**
     * Send an error message back to the Mdr out Queue.
     *
     * @param message
     */
    private void sendErrorMessageToMdrQueue(String message){
        try {
            log.error(message);
            mdrResponseQueueProducer.sendModuleMessage(MdrModuleMapper.createFluxMdrGetCodeListErrorResponse(message), null);
        } catch (MdrModelMarshallException | MessageException e) {
           log.error("Something went wrong during sending of error message back to MdrQueue out! Couldn't recover anymore from this! Response will not be posted!", e);
        }
    }

    private boolean isAcnowledgeMessage(String jmsMessage) {
        if (StringUtils.isBlank(jmsMessage)) {
            return false;
        }
        if (jmsMessage.contains("ACK") && jmsMessage.contains("Acknowledge Of Receipt")) {
            return true;
        }
        return false;
    }

    /**
     * ResponseType from Flux Response.
     *
     * @param textMessage
     * @return ResponseType
     */
    private FLUXMDRReturnMessage extractMdrFluxResponseFromEventMessage(String textMessage) throws MdrModelMarshallException {
        FLUXMDRReturnMessage respType = null;
        try {
            SetFLUXMDRSyncMessageResponse mdrResp = JAXBMarshaller.unmarshallTextMessage(textMessage, SetFLUXMDRSyncMessageResponse.class);
            respType = JAXBMarshaller.unmarshallTextMessage(mdrResp.getRequest(), FLUXMDRReturnMessage.class);
        } catch (MdrModelMarshallException e) {
            log.error(">> Error while attempting to Unmarshall Flux Response Object (XML MDR Entity) : \n", e.getMessage());
        }
        log.info("FluxMdrReturnMessage Unmarshalled successfully.. Going to save the data received! /n");
        return respType;
    }

    /**
     * MdrGetCodeListRequest from Flux Response.
     *
     * @param textMessage
     * @return ResponseType
     */
    private MdrGetCodeListRequest extractMdrGetCodeListEventMessage(String textMessage) throws MdrModelMarshallException {
        MdrGetCodeListRequest codelistReq = null;
        try {
            codelistReq = JAXBMarshaller.unmarshallTextMessage(textMessage, SetFLUXMDRSyncMessageResponse.class);
        } catch (MdrModelMarshallException e) {
            log.error(">> Error while attempting to Unmarshall MdrGetCodeListRequest Object (XML MDR Request) : \n", e.getMessage());
        }
        log.info("MdrGetCodeListRequest Unmarshalled successfully.. Going to validate and get the data now! /n");
        return codelistReq;
    }

    /**
     * Extracts the message content from the EventMessage wrapper.
     *
     * @param eventMessage
     * @return textMessage
     */
    private String extractMessageRequestString(EventMessage eventMessage) {
        String textMessage = null;
        try {
            textMessage = eventMessage.getJmsMessage().getText();
        } catch (JMSException e) {
            log.error("Error : The message is null or empty!");
        }
        return textMessage;
    }

}
