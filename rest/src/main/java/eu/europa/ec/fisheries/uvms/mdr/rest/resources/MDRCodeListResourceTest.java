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

import static org.jgroups.util.Util.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import eu.europa.ec.fisheries.mdr.repository.MdrLuceneSearchRepository;
import eu.europa.ec.fisheries.uvms.exception.ServiceException;
import eu.europa.ec.fisheries.uvms.rest.dto.PaginatedResponse;
import eu.europa.ec.fisheries.uvms.rest.dto.PaginationDto;
import eu.europa.ec.fisheries.uvms.rest.dto.ResponseDto;
import eu.europa.ec.fisheries.uvms.rest.dto.SearchRequestDto;
import eu.europa.ec.fisheries.uvms.rest.dto.SortingDto;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.reflection.Whitebox;

/**
 * Created by georgige on 11/24/2016.
 */
public class MDRCodeListResourceTest {

    @InjectMocks
    private MDRCodeListResource codeListResource;

    @Mock
    private MdrLuceneSearchRepository mdrRepositoryMock;

    @Mock
    private HttpServletRequest requestMock;
    
    private static final String SOME_TEXT_STR = "someText";

    @Before
    public void setUp() {
        codeListResource = new MDRCodeListResource();
        MockitoAnnotations.initMocks(this);
        Whitebox.setInternalState(codeListResource, "mdrService", mdrRepositoryMock);
    }

    @After
    public void tearDown() {
        codeListResource = null;
    }

    @Test
    public void findCodeListByAcronymFilterredByFilterMissingRequiredParam1() {
        SearchRequestDto requestDto = mockSearchRequestDto();
        requestDto.getCriteria().remove("acronym");
        Response response = codeListResource.findCodeListByAcronymFilterredByFilter(requestMock, requestDto);
        Object entity = response.getEntity();
        assertTrue(entity instanceof PaginatedResponse);
        assertEquals("missing_required_parameter_acronym", ((PaginatedResponse)entity).getMsg());
        assertEquals(500, ((PaginatedResponse)entity).getCode());
    }

    @Test
    public void findCodeListByAcronymFilterredByFilterMissingRequiredParam3() {
        SearchRequestDto requestDto = mockSearchRequestDto();
        requestDto.setCriteria(null);
        Response response = codeListResource.findCodeListByAcronymFilterredByFilter(requestMock, requestDto);
        Object entity = response.getEntity();
        assertTrue(entity instanceof ResponseDto);
        assertEquals("missing_required_criteria", ((ResponseDto)entity).getMsg());
        assertEquals(500, ((ResponseDto)entity).getCode());
    }


    @Test
    public void findCodeListByAcronymFilterredByFilterSUCCESS() throws ServiceException {
        SearchRequestDto requestDto = mockSearchRequestDto();
        Response response = codeListResource.findCodeListByAcronymFilterredByFilter(requestMock, requestDto);
        verify(mdrRepositoryMock, times(1)).findCodeListItemsByAcronymAndFilter("TEST", 0, 100, "column_name", true, SOME_TEXT_STR, new String[]{SOME_TEXT_STR});
        verify(mdrRepositoryMock, times(1)).countCodeListItemsByAcronymAndFilter("TEST", SOME_TEXT_STR, new String[]{SOME_TEXT_STR});
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
        criteria.put("filter", SOME_TEXT_STR);
        criteria.put("searchAttribute", new ArrayList<String>(){{add(SOME_TEXT_STR);}});

        requestDto.setCriteria(criteria);

        return requestDto;
    }
}