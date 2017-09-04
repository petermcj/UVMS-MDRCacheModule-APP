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
import eu.europa.ec.fisheries.mdr.mapper.MasterDataRegistryEntityCacheFactory;
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
import java.math.BigInteger;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.jms.JMSException;
import javax.jms.TextMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import un.unece.uncefact.data.standard.mdr.communication.MdrGetCodeListRequest;
import un.unece.uncefact.data.standard.mdr.communication.SetFLUXMDRSyncMessageResponse;
import un.unece.uncefact.data.standard.mdr.communication.ValidationResultType;
import un.unece.uncefact.data.standard.mdr.response.FLUXMDRReturnMessage;

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

    @EJB
    private MdrRepository mdrRepository;


    @EJB
    private MdrLuceneSearchRepository mdrSearchRepositroy;

    @EJB
    MdrQueueProducer mdrResponseQueueProducer;

    private static final String STAR = "*";
    private static final String MDR_MODEL_MARSHALL_EXCEPTION = "MdrModelMarshallException while unmarshalling message from flux : ";
    private static final String ERROR_GET_LIST_FOR_THE_REQUESTED_CODE = "Error while trying to get list for the requested CodeList : ";
    private static final String ACRONYM_DOESNT_EXIST = "The acronym you are searching for does not exist!";

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
     * This method serves the request of getting a list (CodeList)
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
            if (!requestIsOk(message, requestObj)) {
                return;
            }
            // Check query, Run query, Create response
            List<String> columnFilters = requestObj.getColumnsToFilters();
            String filter = requestObj.getFilter();
            String[] columnFiltersArr;
            if(CollectionUtils.isNotEmpty(columnFilters)){
                columnFiltersArr = columnFilters.toArray(new String[columnFilters.size()]);
            } else {
                log.warn("No search attributes provided. Going to consider only 'code' attribute.");
                columnFiltersArr = new String[]{"code", "description"};
            }
            if(filter != null && !filter.equals(STAR)){
                filter = new StringBuilder(STAR).append(filter).append(STAR).toString();
            } else {
                filter = STAR;
            }
            BigInteger wantedNumberOfResults = requestObj.getWantedNumberOfResults();
            Integer nrOfResults = wantedNumberOfResults != null ? wantedNumberOfResults.intValue() : 9999999;

            List<? extends MasterDataRegistry> mdrList = mdrSearchRepositroy.findCodeListItemsByAcronymAndFilter(requestObj.getAcronym(),
                    0, nrOfResults, null, false, filter, columnFiltersArr);
            String validationStr = "Validation is OK.";
            ValidationResultType validation = ValidationResultType.OK;
            if(CollectionUtils.isEmpty(mdrList)){
                validationStr = "Codelist was found but, the search criteria returned 0 results. (Maybe the Table is empty!)";
                validation = ValidationResultType.WOK;
            }
            String mdrGetCodeListResponse = MdrModuleMapper.createFluxMdrGetCodeListResponse(mdrList, requestObj.getAcronym(), validation, validationStr);
            mdrResponseQueueProducer.sendModuleResponseMessage(message.getJmsMessage(), mdrGetCodeListResponse, "MDR");
        } catch (MdrModelMarshallException e) {
            sendErrorMessageToMdrQueue(MDR_MODEL_MARSHALL_EXCEPTION + e, message.getJmsMessage());
        } catch (ServiceException e) {
            sendErrorMessageToMdrQueue(ERROR_GET_LIST_FOR_THE_REQUESTED_CODE + e, message.getJmsMessage());
        }
    }

    /**
     * Checks if the request received for the code list has the minimum requirements.
     *
     * @param message
     * @param requestObj
     * @return
     */
    private boolean requestIsOk(EventMessage message, MdrGetCodeListRequest requestObj) {
        if (requestObj == null || StringUtils.isEmpty(requestObj.getAcronym())) {
            log.error("The message received is not of type MdrGetCodeListRequest so it won't be attempted to unmarshall it! " +
                    "Message content is as follows : " + extractMessageRequestString(message));
            sendErrorMessageToMdrQueue("Request object or Acronym for the request has not been specified!", message.getJmsMessage());
            return false;
        }
        // Acronym doesn't exist
        if(!MasterDataRegistryEntityCacheFactory.getInstance().existsAcronym(requestObj.getAcronym())){
            sendErrorMessageToMdrQueue(ACRONYM_DOESNT_EXIST, message.getJmsMessage());
            return false;
        }
        return true;
    }

    /**
     * Send an error message back to the Mdr out Queue.
     *
     * @param textMessage
     */
    private void sendErrorMessageToMdrQueue(String textMessage, TextMessage jmsMessage){
        try {
            log.error(textMessage);
            mdrResponseQueueProducer.sendModuleResponseMessage(jmsMessage, MdrModuleMapper.createFluxMdrGetCodeListErrorResponse(textMessage), "MDR");
        } catch (MdrModelMarshallException e) {
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
            log.error(">> Error while attempting to Unmarshall Flux Response Object (XML MDR Entity) : \n", e);
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
            log.error(">> Error while attempting to Unmarshall MdrGetCodeListRequest Object (XML MDR Request) : \n", e);
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
            log.error("Error : The message is null or empty!", e);
        }
        return textMessage;
    }

}
