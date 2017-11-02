/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.

*/
package eu.europa.ec.fisheries.uvms.mdr.rest.resources;

import javax.ejb.EJB;
import javax.interceptor.Interceptors;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import eu.europa.ec.fisheries.mdr.repository.MdrLuceneSearchRepository;
import eu.europa.ec.fisheries.uvms.commons.rest.resource.UnionVMSResource;
import eu.europa.ec.fisheries.uvms.mdr.rest.resources.util.IUserRoleInterceptor;
import eu.europa.ec.fisheries.uvms.mdr.rest.resources.util.MdrExceptionInterceptor;
import lombok.extern.slf4j.Slf4j;
import un.unece.uncefact.data.standard.mdr.communication.MdrFeaturesEnum;

/**
 * Created by kovian on 19/04/2017.
 */
@Slf4j
@Path("/index")
public class MdrLuceneReindexingResource extends UnionVMSResource {

    @EJB
    MdrLuceneSearchRepository searchRepository;

    /**
     * Reindex Lucene indexes in order to have results when for Example the db has been loaded from a back-up,
     * and genericaly when the data has not been saved by hibernate, but in some other way (Like : Liquibase).
     *
     * @param request
     * @return
     */
    @GET
    @Path("/reindex")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Interceptors(MdrExceptionInterceptor.class)
    @IUserRoleInterceptor(requiredUserRole = {MdrFeaturesEnum.MDR_LUCENE_REINDEX})
    public Response findCodeListByAcronymFilterredByFilter(@Context HttpServletRequest request) {
        final boolean[] error = {false};
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    searchRepository.massiveUpdateFullTextIndex();
                } catch (InterruptedException e) {
                    error[0] = true;
                }
            }
        }).start();
        if(error[0]){
            return createErrorResponse("Error while trying to massive reindex Lucene indexes.");
        }
        return createSuccessResponse("Successfully massively reindexing MDR lucene indexes. The process is asynchronous, so to see the changes" +
                " depending on the size of the db it could take from some seconds to some minutes!");
    }
}
