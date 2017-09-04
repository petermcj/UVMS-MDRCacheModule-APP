/*
 *
 * Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries © European Union, 2015-2016.
 *
 * This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package eu.europa.ec.fisheries.uvms.mdr.rest.resources;

import eu.europa.ec.fisheries.mdr.entities.codelists.baseentities.MasterDataRegistry;
import eu.europa.ec.fisheries.mdr.mapper.MasterDataRegistryEntityCacheFactory;
import eu.europa.ec.fisheries.mdr.repository.MdrLuceneSearchRepository;
import eu.europa.ec.fisheries.uvms.exception.ServiceException;
import eu.europa.ec.fisheries.uvms.mdr.rest.resources.util.IUserRoleInterceptor;
import eu.europa.ec.fisheries.uvms.mdr.rest.resources.util.MdrExceptionInterceptor;
import eu.europa.ec.fisheries.uvms.rest.dto.PaginationDto;
import eu.europa.ec.fisheries.uvms.rest.dto.SearchRequestDto;
import eu.europa.ec.fisheries.uvms.rest.dto.SortingDto;
import eu.europa.ec.fisheries.uvms.rest.resource.UnionVMSResource;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.interceptor.Interceptors;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import un.unece.uncefact.data.standard.mdr.communication.MdrFeaturesEnum;

/**
 * Created by georgige on 8/22/2016.
 */
@Slf4j
@Path("/cl")
public class MDRCodeListResource extends UnionVMSResource {

    @EJB
    private MdrLuceneSearchRepository mdrSearchRepositroy;


    @POST
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Interceptors(MdrExceptionInterceptor.class)
    @IUserRoleInterceptor(requiredUserRole = {MdrFeaturesEnum.MDR_SEARCH_CODE_LIST_ITEMS})
    public Response findCodeListByAcronymFilterredByFilter(@Context HttpServletRequest request, SearchRequestDto searchRequest) {
        Response response;
        Map<String, Object> criteria = searchRequest.getCriteria();
        if (!MapUtils.isEmpty(criteria)) {
            String acronym           = (String) criteria.get("acronym");
            PaginationDto pagination = searchRequest.getPagination();
            int offset               = pagination!=null?pagination.getOffset():0;
            int pageSize             = pagination!=null?pagination.getPageSize():Integer.MAX_VALUE;
            SortingDto sorting       = searchRequest.getSorting();
            String sortBy            = sorting!=null? sorting.getSortBy():null;
            boolean isReversed       = sorting!=null? sorting.isReversed():false;
            String filter            = (String) criteria.get("filter");
            String[] searchAttributes = getSearchAttributesAsArray(criteria.get("searchAttribute"));
            try {
                response = this.findCodeListByAcronymFilterredByFilter(request, acronym, offset, pageSize, sortBy, isReversed, filter, searchAttributes);
            } catch (NumberFormatException e) {
                log.error("Internal Server Error.", e);
                response = createErrorResponse("internal_server_error" + e);
            }

        } else {
            response = createErrorResponse("missing_required_criteria");
        }
        return response;
    }

    @NotNull
    private String[] getSearchAttributesAsArray(Object attributesObj){
        if(attributesObj == null){
            return new String[0];
        }
        List<String> searchAttributeList = (List<String>) attributesObj;
        String[] searchAttributes        = new String[searchAttributeList.size()];
        searchAttributeList.toArray(searchAttributes);
        return searchAttributes;
    }

    @GET
    @Path("/{acronym}/{offset}/{pageSize}")
    @Produces(MediaType.APPLICATION_JSON)
    @Interceptors(MdrExceptionInterceptor.class)
    @IUserRoleInterceptor(requiredUserRole = {MdrFeaturesEnum.MDR_SEARCH_CODE_LIST_ITEMS})
    public Response findCodeListByAcronymFilterredByFilter(@Context HttpServletRequest request,
                                                            @PathParam("acronym") String acronym,
                                                            @PathParam("offset") Integer offset,
                                                            @PathParam("pageSize") Integer pageSize,
                                                            @QueryParam("sortBy") String sortBy,
                                                            @QueryParam("sortReversed") Boolean isReversed,
                                                            @QueryParam("filter") String filter,
                                                            @QueryParam("searchAttributes") String[] searchAttributes) {
        log.debug("findCodeListByAcronymFilterredByFilter(acronym={}, offset={}, pageSize={}, sortBy={}, isReversed={}, filter={}, searchAttribute={})", acronym,offset,pageSize,sortBy,isReversed,filter, searchAttributes);
        try {
            if(searchAttributes == null || searchAttributes.length == 0){
                searchAttributes = new String[]{"code"};
                log.warn("No search attributes provide. Going to consider only 'code' attribute.");
            }
            if(!MasterDataRegistryEntityCacheFactory.getInstance().existsAcronym(acronym)){
                createErrorResponse("The acronym you are searching for doesn't exist in MDR cache.");
            }
            if(StringUtils.isEmpty(filter)){
                createErrorResponse("Filter attribute cannot be empty.");
            }
            List<? extends MasterDataRegistry> mdrList = mdrSearchRepositroy.findCodeListItemsByAcronymAndFilter(acronym, offset, pageSize, sortBy, isReversed, filter, searchAttributes);
            int totalCodeItemsCount = mdrSearchRepositroy.countCodeListItemsByAcronymAndFilter(acronym, filter, searchAttributes);
            return createSuccessPaginatedResponse(mdrList, totalCodeItemsCount);
        } catch (ServiceException e) {
            log.error("Internal Server Error.", e);
            return createErrorResponse("internal_server_error :" + e);
        }
    }

}
