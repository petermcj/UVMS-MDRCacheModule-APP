/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.

*/
package eu.europa.ec.fisheries.mdr.repository.bean;

import eu.europa.ec.fisheries.mdr.entities.codelists.baseentities.MasterDataRegistry;
import eu.europa.ec.fisheries.mdr.exception.MdrCacheInitException;
import eu.europa.ec.fisheries.mdr.mapper.MasterDataRegistryEntityCacheFactory;
import eu.europa.ec.fisheries.mdr.repository.MdrLuceneSearchRepository;
import eu.europa.ec.fisheries.mdr.service.bean.BaseMdrBean;
import eu.europa.ec.fisheries.uvms.commons.service.exception.ServiceException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;

/**
 * Created by kovian on 15/12/2016.
 */
@Stateless
@Slf4j
public class MdrLuceneSearchRepositoryBean extends BaseMdrBean implements MdrLuceneSearchRepository {

    private FullTextEntityManager fullTextEntityManager;

    @PostConstruct
    public void init() {
        initEntityManager();
        fullTextEntityManager = getFullTextEntityManager();
    }


    /**
     * This method is searching for code list items for a given code list by its acronym. The search is using Hibernate Search API, which is based on Lucene indexing, for high performance.
     *
     * @param acronym          of the code list which the method is filtering. [Mandatory parameter]
     * @param offset           is the number of the first returned element
     * @param pageSize         is the total number of items to be returned
     * @param sortBy           is a field name which will be used for searching
     * @param isReversed       is a boolean flag that defines whether the sorting is reversed
     * @param filter           is a free text string that is used for code lists search
     * @param searchAttributes if filter is specified, this field is mandatory. It's an array of all fields that will be used for filtering
     * @return a list of code list items (instances of MasterDataRegistry class)
     * @throws ServiceException
     */
    @Override
    public List<? extends MasterDataRegistry> findCodeListItemsByAcronymAndFilter(
            String acronym, Integer offset, Integer pageSize, String sortBy,
            Boolean isReversed, String filter, String... searchAttributes) throws ServiceException {

        if(searchAttributes == null || searchAttributes.length == 0){
            searchAttributes = new String[]{"code", "description"};
            log.warn("No search attributes provide. Going to consider only 'code' attribute.");
        }

        // Build fullTextQuery;
        FullTextQuery fullTextQuery = buildLuceneMdrQuery(acronym, filter, searchAttributes);
        // SetUp the query properties and get the resultList from it;
        return setUpQueryProperties(offset, pageSize, sortBy, isReversed, fullTextQuery).getResultList();
    }

    /**
     * This method is searching for code list items for a given code list by its acronym. The search is using Hibernate Search API, which is based on Lucene indexing, for high performance.
     *
     * @param acronym          of the code list which the method is filtering. [Mandatory parameter]
     * @param filter           is a free text string that is used for code lists search
     * @param searchAttributes if filter is specified, this field is mandatory. It's an array of all fields that will be used for filtering
     * @return a the total count of search results
     * @throws ServiceException
     */
    @Override
    public int countCodeListItemsByAcronymAndFilter(String acronym, String filter, String... searchAttributes) throws ServiceException {
        log.debug("[START] countCodeListItemsByAcronymAndFilter(acronym=[{}], offset=[{}], pageSize=[{}], sortBy=[{}], isReversed=[{}], filter=[{}], searchOnAttribute=[{}])");
        FullTextQuery fullTextQuery = buildLuceneMdrQuery(acronym, filter, searchAttributes);
        log.debug("[END] countCodeListItemsByAcronymAndFilter(...)");
        return fullTextQuery.getResultSize();
    }


    /**
     * Builds the fullText query based on acronym, textToSearch and attributes to search on.
     *
     * @param acronym
     * @param filterText
     * @param searchAttributes
     * @return
     * @throws ServiceException
     */
    private FullTextQuery buildLuceneMdrQuery(String acronym, String filterText, String... searchAttributes) throws ServiceException {
        // Check the minimum required fields for search are provided;
        FullTextQuery fullTextQuery;
        try {
            checkAcronymFilterAndSearchTextAreProvided(acronym, filterText);
            Class codeListClass = MasterDataRegistryEntityCacheFactory.getInstance().getNewInstanceForEntity(acronym).getClass();
            FullTextEntityManager ftEntityManager = getFullTextEntityManager();
            QueryBuilder queryBuilder = ftEntityManager.getSearchFactory().buildQueryBuilder().forEntity(codeListClass).get();
            Query luceneQuery = queryBuilder.keyword().wildcard().onFields(searchAttributes).ignoreFieldBridge().matching(filterText.toLowerCase()).createQuery();
            fullTextQuery = ftEntityManager.createFullTextQuery(luceneQuery, codeListClass);
            log.debug("Using lucene query: {}", fullTextQuery.toString());
        } catch (MdrCacheInitException e) {
            throw new ServiceException("Unable to execute search query due to internal server error.", e);
        } catch (IllegalArgumentException e) {
            throw new ServiceException(e.getMessage(), e);
        }
        return fullTextQuery;
    }

    /**
     * To be used in the future when required search for multiple words (aka phrase search).
     *
     * @param acronym
     * @param filterText
     * @param searchAttributes
     * @return
     * @throws ServiceException
     */
    private FullTextQuery buildLuceneMdrPhraseQuery(String acronym, String filterText, String... searchAttributes) throws ServiceException {
        // Check the minimum required fields for search are provided;
        checkAcronymFilterAndSearchTextAreProvided(acronym, filterText);
        FullTextQuery fullTextQuery;
        try {

            // Split the searched phrase in multiple words to search for.
            List<String> keyWords = Arrays.asList(filterText.split(StringUtils.SPACE));
            Class mdrClass = MasterDataRegistryEntityCacheFactory.getInstance().getNewInstanceForEntity(acronym).getClass();
            List<Query> queryList = new LinkedList<>();
            for (String fieldName : searchAttributes) {
                PhraseQuery phraseQuery = new PhraseQuery();
                for (String keyWord : keyWords) {
                    phraseQuery.add(new Term(fieldName, keyWord));
                }
                // How many words between are tolerated.
                phraseQuery.setSlop(4);
                queryList.add(phraseQuery);
            }
            BooleanQuery finalQuery = new BooleanQuery();
            for (Query actQuery : queryList) {
                finalQuery.add(actQuery, BooleanClause.Occur.MUST);
            }
            fullTextQuery = getFullTextEntityManager().createFullTextQuery(finalQuery, mdrClass);
            log.debug("Using lucene query: {}", fullTextQuery.toString());
        } catch (IllegalArgumentException | MdrCacheInitException e) {
            throw new ServiceException("Unable to execute search query due to internal server error.", e);
        }
        return fullTextQuery;
    }


    /**
     * Sets up the offset, pageSize and sortBy properties to the query object.
     *
     * @param offset
     * @param pageSize
     * @param sortBy
     * @param isReversed
     * @param query
     */
    private FullTextQuery setUpQueryProperties(Integer offset, Integer pageSize, String sortBy, Boolean isReversed, FullTextQuery query) {
        if (offset != null) {
            query.setFirstResult(offset);
        }
        if (pageSize != null) {
            query.setMaxResults(pageSize);
        }
        if (StringUtils.isNotBlank(sortBy)) {
            SortField.Type sortType = SortField.Type.STRING;
            if("validity.startDate".equalsIgnoreCase(sortBy) || "validity.endDate".equalsIgnoreCase(sortBy)){
                log.info("[INFO] Sorting by date...");
                sortType = SortField.Type.LONG;
            }
            query.setSort(new Sort(new SortField(sortBy, sortType, isReversed)));
        }
        return query;
    }


    /**
     * Checks that at least acronym and text to filter from search fields are provided;
     *
     * @param acronym
     * @param filterText
     */
    private void checkAcronymFilterAndSearchTextAreProvided(String acronym, String filterText) throws IllegalArgumentException {
        if (StringUtils.isBlank(acronym)) {
            throw new IllegalArgumentException("No acronym parameter is provided.");
        }
        if (StringUtils.isBlank(filterText)) {
            throw new IllegalArgumentException("No search attributes are provided.");
        }
    }


    /**
     * Convenience method to get Full Text Entity Manager.
     * Protected scope to assist mocking in Unit Tests.
     *
     * @return Full Text Entity Manager.
     */
    protected FullTextEntityManager getFullTextEntityManager() {
        if (fullTextEntityManager == null || !fullTextEntityManager.isOpen()) {
            fullTextEntityManager = Search.getFullTextEntityManager(getEntityManager());
        }
        return Search.getFullTextEntityManager(getEntityManager());
    }


    /**
     * Method to manually update the Full Text Index. This is not required if inserting entities
     * using this Manager as they will automatically be indexed. Useful though if you need to index
     * data inserted using a different method (e.g. pre-existing data, or test data inserted via
     * scripts or DbUnit).
     */
    @Override
    public void massiveUpdateFullTextIndex() throws InterruptedException {
        log.info("Updating Lucene Index for MDR module..");
        getFullTextEntityManager().createIndexer().startAndWait();
    }

}
