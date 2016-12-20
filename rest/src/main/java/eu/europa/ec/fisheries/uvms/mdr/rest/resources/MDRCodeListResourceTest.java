/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.

*/
package eu.europa.ec.fisheries.uvms.mdr.rest.resources;

/**
 * Created by kovian on 16/12/2016.
 */

import eu.europa.ec.fisheries.mdr.repository.MdrLuceneSearchRepository;
import eu.europa.ec.fisheries.uvms.rest.dto.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.reflection.Whitebox;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

import static org.jgroups.util.Util.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by georgige on 11/24/2016.
 */
@Ignore
public class MDRCodeListResourceTest {

    @InjectMocks
    private MDRCodeListResource codeListResource;

    @Mock
    private MdrLuceneSearchRepository mdrRepositoryMock;

    @Mock
    private HttpServletRequest requestMock;

    @Before
    public void setUp() throws Exception {
        codeListResource = new MDRCodeListResource();
        MockitoAnnotations.initMocks(this);
        Whitebox.setInternalState(codeListResource, "mdrService", mdrRepositoryMock);
    }

    @After
    public void tearDown() throws Exception {
        codeListResource = null;
    }

    @Test
    public void findCodeListByAcronymFilterredByFilterMissingRequiredParam1() throws Exception {
        SearchRequestDto requestDto = mockSearchRequestDto();
        requestDto.getCriteria().remove("acronym");
        Response response = codeListResource.findCodeListByAcronymFilterredByFilter(requestMock, requestDto);
        Object entity = response.getEntity();
        assertTrue(entity instanceof ResponseDto);
        assertEquals("missing_required_parameter_acronym", ((ResponseDto)entity).getMsg());
        assertEquals(500, ((ResponseDto)entity).getCode());
    }

    @Test
    public void findCodeListByAcronymFilterredByFilterMissingRequiredParam3() throws Exception {
        SearchRequestDto requestDto = mockSearchRequestDto();
        requestDto.setCriteria(null);
        Response response = codeListResource.findCodeListByAcronymFilterredByFilter(requestMock, requestDto);
        Object entity = response.getEntity();
        assertTrue(entity instanceof ResponseDto);
        assertEquals("missing_required_criteria", ((ResponseDto)entity).getMsg());
        assertEquals(500, ((ResponseDto)entity).getCode());
    }


    @Test
    public void findCodeListByAcronymFilterredByFilterSUCCESS() throws Exception {
        SearchRequestDto requestDto = mockSearchRequestDto();
        Response response = codeListResource.findCodeListByAcronymFilterredByFilter(requestMock, requestDto);
        verify(mdrRepositoryMock, times(1)).findCodeListItemsByAcronymAndFilter("TEST", 0, 100, "column_name", true, "someText", new String[]{"someText"});
        verify(mdrRepositoryMock, times(1)).countCodeListItemsByAcronymAndFilter("TEST", "someText", new String[]{"someText"});
        Object entity = response.getEntity();
        assertTrue(entity instanceof PaginatedResponse);
        assertEquals(200, ((PaginatedResponse)entity).getCode());
        assertEquals(0, ((PaginatedResponse)entity).getTotalItemsCount());
        assertEquals(0, ((PaginatedResponse)entity).getResultList().size());
    }

    private SearchRequestDto mockSearchRequestDto() {
        SearchRequestDto requestDto = new SearchRequestDto();
        PaginationDto paginationDto = new PaginationDto();
        paginationDto.setOffset(0);
        paginationDto.setPageSize(100);

        SortingDto sortingDto = new SortingDto();
        sortingDto.setReversed(true);
        sortingDto.setSortBy("column_name");

        requestDto.setPagination(paginationDto);
        requestDto.setSorting(sortingDto);

        Map<String, Object> criteria = new HashMap<>();
        criteria.put("acronym", "TEST");
        criteria.put("filter", "someText");
        criteria.put("searchAttribute", new String[]{"someText"});

        requestDto.setCriteria(criteria);

        return requestDto;
    }
}