/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it 
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of 
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more 
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.

 */
package eu.europa.ec.fisheries.mdr.dao;

import eu.europa.ec.fisheries.mdr.entities.codelists.baseentities.MasterDataRegistry;
import eu.europa.ec.fisheries.uvms.exception.ServiceException;
import java.util.List;
import javax.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.jpa.Search;

/***
 * This class is used only for bulk insertions.
 */
@Slf4j
public class MdrBulkOperationsDao {

    private EntityManager em;

    private static final String HQL_DELETE = "DELETE FROM ";

    public MdrBulkOperationsDao() {
        super();
    }

    public MdrBulkOperationsDao(EntityManager em) {
        this.em = em;
    }


    /**
     * Purges the Lucene index before deletion and insertion of the new entries.
     * Deletes all entries of all the given Entities and then inserts all the new ones.
     * The input is the list of all the entities ('Rows' of the same Entity) ready to be persisted (each entity contains one or more records).
     *
     * @param entityRows
     * @throws ServiceException
     */
    public void singleEntityBulkDeleteAndInsert(List<? extends MasterDataRegistry> entityRows) throws ServiceException {

        if (!CollectionUtils.isEmpty(entityRows)) {

            Class mdrClass       = entityRows.get(0).getClass();
            String mdrEntityName = mdrClass.getSimpleName();

            // Deletion and purging of all entries of this Mdr Entity.
            deleteFromDbAndPurgeAllFromIndex(mdrEntityName, mdrClass);

            // Refreshing Lucene indexes and storing data to DB.
            log.info("Rebuilding Lucene index for entity : " + mdrEntityName + "...");
            saveNewEntriesAndRefreshLuceneIndexes(mdrClass, entityRows);
        }

    }

    /**
     * Delets all the entries from this Entity and inserts the new ones.
     *
     * @param entityName
     * @param mdrClass
     * @throws InterruptedException
     */
    public void deleteFromDbAndPurgeAllFromIndex(String entityName, Class mdrClass) throws ServiceException {
        FullTextSession fullTextSession = getFullTextSession();
        Transaction ftTx                = fullTextSession.beginTransaction();
        try {
            log.info("Deleting and purging entity entries for : {}", entityName);
            // DELETION PHASE (Deleting old entries)
            String query = new StringBuilder(HQL_DELETE).append(entityName).toString();
            fullTextSession.createQuery(query).executeUpdate();

            // Purging old indexes
            fullTextSession.purgeAll(mdrClass);  // Remove obsolete content
            fullTextSession.flushToIndexes();    // Apply purge now, before optimize
            fullTextSession.getSearchFactory().optimize(mdrClass);
            ftTx.commit();
            log.info("Deletion and purging-all for {} completed.", mdrClass.toString());
        } catch (Exception e) {
            ftTx.rollback();
            throw new ServiceException("Rollbacking transaction for reason : ", e);
        } finally {
            log.debug("Closing session");
            fullTextSession.close();
        }
    }


    /**
     * Refreshes the Lucene indexes with the latest deletions and insertions.
     *
     * @param mdrClass
     * @param entityRows
     * @throws InterruptedException
     */
    public void saveNewEntriesAndRefreshLuceneIndexes(Class mdrClass, List<? extends MasterDataRegistry> entityRows) throws ServiceException {
        FullTextSession fullTextSession = getFullTextSession();
        Transaction tx  = fullTextSession.beginTransaction();
        try {
            log.info("Saving all entity entries for Acronym : ", entityRows.get(0).getAcronym());
            for (MasterDataRegistry actualEnityRow : entityRows) {
                fullTextSession.save(actualEnityRow);
            }
            fullTextSession.flush();
            fullTextSession.clear();
            tx.commit();
            log.info("Insertion for {} completed.", mdrClass.toString());
        } catch (Exception e) {
            tx.rollback();
            throw new ServiceException("Rollbacking transaction for reason : ", e);
        } finally {
            log.debug("Closing session");
            fullTextSession.close();
        }
    }


    /**
     * Unwraps a JPA Session.
     *
     * @return session;
     */
    private Session getJpaSession() {
        return (getEntityManager().unwrap(Session.class)).getSessionFactory().openSession();
    }

    /**
     * Unwraps a full text Hibernate Search Session.
     *
     * @return session;
     */
    private FullTextSession getFullTextSession(){
        return org.hibernate.search.Search.getFullTextSession(getJpaSession());
    }

    /**
     * Returns the number of "documents" (aka rows) of a certain entity;
     *
     * @param mdrClass
     * @return nrOfDocuments
     */
    public int getDocsNumberForEntity(Class mdrClass){
        FullTextSession session = getFullTextSession();
        return session.getSearchFactory().getIndexReaderAccessor().open(mdrClass).numDocs();
    }

    /**
     * Massivly deletes all the previous indexes and depending on the annotated classes (entities)
     * it recreates the whole index (Ps : use with caution)
     *
     * @throws InterruptedException
     */
    public void massiveFlushAndReIndex() throws InterruptedException {
        Search.getFullTextEntityManager(em).createIndexer().startAndWait();
    }

    /**
     * Purges all the Rows of the given entity from the lucene Index.
     *
     */
    public void purgeAllForEntity(Class mdrEntityClass){
        Search.getFullTextEntityManager(em).purgeAll(mdrEntityClass);
    }

    public EntityManager getEntityManager() {
        return em;
    }
}