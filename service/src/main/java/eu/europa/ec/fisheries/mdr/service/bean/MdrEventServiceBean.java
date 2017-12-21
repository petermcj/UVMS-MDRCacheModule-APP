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
import eu.europa.ec.fisheries.mdr.exception.MdrCacheInitException;
import eu.europa.ec.fisheries.mdr.mapper.MasterDataRegistryEntityCacheFactory;
import eu.europa.ec.fisheries.mdr.repository.MdrLuceneSearchRepository;
import eu.europa.ec.fisheries.mdr.repository.MdrRepository;
import eu.europa.ec.fisheries.mdr.service.MdrEventService;
import eu.europa.ec.fisheries.uvms.commons.message.impl.JAXBUtils;
import eu.europa.ec.fisheries.uvms.commons.service.exception.ServiceException;
import eu.europa.ec.fisheries.uvms.mdr.message.event.GetAllMdrCodeListsMessageEvent;
import eu.europa.ec.fisheries.uvms.mdr.message.event.GetSingleMdrListMessageEvent;
import eu.europa.ec.fisheries.uvms.mdr.message.event.MdrSyncMessageEvent;
import eu.europa.ec.fisheries.uvms.mdr.message.event.carrier.EventMessage;
import eu.europa.ec.fisheries.uvms.mdr.message.producer.commonproducers.MdrQueueProducer;
import eu.europa.ec.fisheries.uvms.mdr.model.exception.MdrModelMarshallException;
import eu.europa.ec.fisheries.uvms.mdr.model.mapper.MdrModuleMapper;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.jms.JMSException;
import javax.jms.TextMessage;
import javax.persistence.Column;
import javax.xml.bind.JAXBException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import un.unece.uncefact.data.standard.mdr.communication.MdrGetCodeListRequest;
import un.unece.uncefact.data.standard.mdr.communication.SetFLUXMDRSyncMessageResponse;
import un.unece.uncefact.data.standard.mdr.communication.SingleCodeListRappresentation;
import un.unece.uncefact.data.standard.mdr.communication.ValidationResultType;
import un.unece.uncefact.data.standard.mdr.response.FLUXMDRReturnMessage;
import un.unece.uncefact.data.standard.mdr.response.FLUXResponseDocumentType;

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
    private static final String ACRONYM_DOESNT_EXIST = "The acronym you are searching for does not exist! Acronym ::: ";

    /**
     * This method saves the received codeList (from FLUX or MANUAL upload).
     *
     * @param message
     */
    @Override
    public void recievedSyncMdrEntityMessage(@Observes @MdrSyncMessageEvent EventMessage message) {
        log.info("[INFO] Recieved message from FLUX related to MDR Entity Synchronization.");
        try {
            String messageStr = extractMessageRequestString(message);
            FLUXMDRReturnMessage responseMessage = extractMdrFluxResponseFromEventMessage(messageStr);
            if (isDataMessage(messageStr, responseMessage)) {
                mdrRepository.updateMdrEntity(responseMessage);
            }
        } catch (MdrModelMarshallException e) {
            log.error("[ERROR] MdrModelMarshallException while unmarshalling message from flux ", e);
        }
    }

    private boolean isDataMessage(String messageStr, FLUXMDRReturnMessage responseMessage) {
        boolean isDataMessage = true;
        if (StringUtils.isEmpty(messageStr)) {
            log.error("[ERROR] The message received is not of type <<FLUXMDRReturnMessage>> so it won't be attempted to save it! Message content is as follows : " + messageStr);
            isDataMessage = false;
        } else if (isAcnowledgeMessage(messageStr)) {
            log.info("[ACKNOWLEDGE] : Received Acnowledge Message for a prevoius request sent to FLUX. No data so nothing is going to be persisted in MDR.");
            isDataMessage = false;
        } else if (isObjDescriptionMessage(responseMessage)) {
            mdrRepository.saveAcronymStructureMessage(messageStr, responseMessage.getMDRDataSet().getID().getValue());
            mdrRepository.updateMetaDataForAcronym(responseMessage.getMDRDataSet());
            isDataMessage = false;
        }
        return isDataMessage;
    }

    private boolean isObjDescriptionMessage(FLUXMDRReturnMessage fluxReturnMessage) {
        FLUXResponseDocumentType fluxResponseDocument = fluxReturnMessage.getFLUXResponseDocument();
        if (fluxResponseDocument != null && fluxResponseDocument.getTypeCode() != null && "OBJ_DESC".equals(fluxResponseDocument.getTypeCode().getValue())) {
            return true;
        }
        return false;
    }

    /**
     * This method serves the request of getting a CodeList and returning it as a jmsMessage.
     *
     * @param message
     */
    @Override
    public void recievedGetSingleMdrCodeListMessage(@Observes @GetSingleMdrListMessageEvent EventMessage message) {
        log.info("[INFO] Recieved GetSingleMDRListMessageEvent.. Going to fetch Lucene Indexes..");
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
            if (CollectionUtils.isNotEmpty(columnFilters)) {
                columnFiltersArr = columnFilters.toArray(new String[columnFilters.size()]);
            } else {
                log.warn("No search attributes provided. Going to consider all 'code' and 'description' attributes.");
                columnFiltersArr = new String[]{"code", "description"}; //getAllFieldsForAcronym(requestObj.getAcronym());
            }
            if (StringUtils.isNotEmpty(filter) && !filter.equals(STAR)) {
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
            if (CollectionUtils.isEmpty(mdrList)) {
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

    @Override
    public void recievedGetAllMdrCodeListMessage(@Observes @GetAllMdrCodeListsMessageEvent EventMessage message) {
        try {
            log.info("[INFO] Got GetAllMdrCodeListsMessageEvent..");
            List<String> acronymsList = null;
            try {
                acronymsList = MasterDataRegistryEntityCacheFactory.getAcronymsList();
            } catch (MdrCacheInitException e) {
                log.error("[ERROR] While trying to get the acronym list (MasterDataRegistryEntityCacheFactory.getAcronymsList())!!");
            }
            List<SingleCodeListRappresentation> allCoceLists = new ArrayList<>();
            for(String actAcronym : acronymsList){
                List<? extends MasterDataRegistry> mdrList = mdrSearchRepositroy.findCodeListItemsByAcronymAndFilter(actAcronym,
                        0, 99999999, "code", false, "*", "code");
                allCoceLists.add(MdrModuleMapper.mapToSingleCodeListRappresentation(mdrList, actAcronym, null, "OK"));
            }
            String response = MdrModuleMapper.mapToMdrGetAllCodeListsResponse(allCoceLists);
            mdrResponseQueueProducer.sendModuleResponseMessage(message.getJmsMessage(), response, "MDR");
        } catch (MdrModelMarshallException e) {
            sendErrorMessageToMdrQueue(MDR_MODEL_MARSHALL_EXCEPTION + e, message.getJmsMessage());
        } catch (ServiceException e) {
            sendErrorMessageToMdrQueue(ERROR_GET_LIST_FOR_THE_REQUESTED_CODE + e, message.getJmsMessage());
        }
    }

    private String[] getAllFieldsForAcronym(String acronym) {
        Field[] fields = null;
        List<String> fieldsList = new ArrayList<String>(){{add("code");add("description");}};
        try {
            MasterDataRegistry newInstanceForEntity = MasterDataRegistryEntityCacheFactory.getInstance().getNewInstanceForEntity(acronym);
            fields = newInstanceForEntity.getClass().getDeclaredFields();
        } catch (MdrCacheInitException e) {
            log.error("[ERROR] Error while trying to get instance for acronym : " + acronym);
        }
        for(Field field : fields){
            if(field.isAnnotationPresent(Column.class) && !"id".equals(field.getName())){
                fieldsList.add(field.getName());
            }
        }
        String[] ausArr = new String[fieldsList.size()];
        return fieldsList.toArray(ausArr);
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
        if (!MasterDataRegistryEntityCacheFactory.getInstance().existsAcronym(requestObj.getAcronym())) {
            sendErrorMessageToMdrQueue(ACRONYM_DOESNT_EXIST + requestObj.getAcronym(), message.getJmsMessage());
            return false;
        }
        return true;
    }

    /**
     * Send an error message back to the Mdr out Queue.
     *
     * @param textMessage
     */
    private void sendErrorMessageToMdrQueue(String textMessage, TextMessage jmsMessage) {
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
            SetFLUXMDRSyncMessageResponse mdrResp = JAXBUtils.unMarshallMessage(textMessage, SetFLUXMDRSyncMessageResponse.class);
            respType = JAXBUtils.unMarshallMessage(mdrResp.getRequest(), FLUXMDRReturnMessage.class);
        } catch (JAXBException e) {
            log.error(">> Error while attempting to Unmarshall Flux Response Object (XML MDR Entity)! Maybe not a FLUXMDRReturnMessage!!");
        }
        log.info("FluxMdrReturnMessage Unmarshalled successfully.. Going to save the data received! \n");
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
            codelistReq = JAXBUtils.unMarshallMessage(textMessage, SetFLUXMDRSyncMessageResponse.class);
        } catch (JAXBException e) {
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
