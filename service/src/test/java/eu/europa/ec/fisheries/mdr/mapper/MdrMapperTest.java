/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.

*/
package eu.europa.ec.fisheries.mdr.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import eu.europa.ec.fisheries.schema.rules.module.v1.SetFLUXMDRSyncMessageRulesRequest;
import eu.europa.ec.fisheries.uvms.commons.message.impl.JAXBUtils;
import lombok.SneakyThrows;
import org.junit.Test;

/**
 * Created by kovian on 02/08/2017.
 */
public class MdrMapperTest {

    private static final String OBJ_DATA_ALL = "OBJ_DATA_ALL";
    private static final String OBJ_DESC = "OBJ_DESC";
    private static final String MEMEBRSTATES_ACRONYM = "MEMBER_STATE";

    @SneakyThrows
    @Test
    public void testIndexRequestCreation(){
        String index = MdrRequestMapper.mapMdrQueryTypeToStringForINDEXServiceType("INDEX");
        final SetFLUXMDRSyncMessageRulesRequest unmarshalled = JAXBUtils.unMarshallMessage(index, SetFLUXMDRSyncMessageRulesRequest.class);
        System.out.print(unmarshalled.getRequest());
    }

    @SneakyThrows
    @Test
    public void testCreationOfUpdateRequest(){
        List<String> existingAcronymsList = MasterDataRegistryEntityCacheFactory.getAcronymsList();
        List<String> requestsList = new ArrayList<>();
        String uuid = java.util.UUID.randomUUID().toString();
        for(String actualAcronym : existingAcronymsList){
            final String req = MdrRequestMapper.mapMdrQueryTypeToString(actualAcronym, OBJ_DATA_ALL, uuid);
            SetFLUXMDRSyncMessageRulesRequest unmarshalledReq = JAXBUtils.unMarshallMessage(req, SetFLUXMDRSyncMessageRulesRequest.class);
            requestsList.add(req);
            System.out.println(unmarshalledReq.getRequest());
        }
    }

    @SneakyThrows
    @Test
    public void testCreationOfStructureServiceRequest(){
        final String request = MdrRequestMapper.mapMdrQueryTypeToString(MEMEBRSTATES_ACRONYM, OBJ_DESC, UUID.randomUUID().toString());
        final SetFLUXMDRSyncMessageRulesRequest unmarshalled = JAXBUtils.unMarshallMessage(request, SetFLUXMDRSyncMessageRulesRequest.class);
        System.out.println(unmarshalled.getRequest());
    }
}


