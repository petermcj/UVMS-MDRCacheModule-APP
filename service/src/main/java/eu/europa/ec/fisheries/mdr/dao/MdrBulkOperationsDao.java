/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it 
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of 
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more 
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.

 */
package eu.europa.ec.fisheries.mdr.dao;

import eu.europa.ec.fisheries.mdr.domain.codelists.base.MasterDataRegistry;
import eu.europa.ec.fisheries.uvms.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.Serializable;
import java.util.List;

/***
 * This class is used only for bulk insertions.
 */
@Slf4j
public class MdrBulkOperationsDao implements Serializable {

    @PersistenceContext(unitName = "mdrPU")
    private EntityManager em;

    private static final String HQL_DELETE = "DELETE FROM ";

    /**
     * Purges the Lucene index before deletion and insertion of the new entries.
     * Deletes all entries of all the given Entities and then inserts all the new ones.
     * The input is the list of all the entities and all their instances (List of tables) ready to be persisted (each entity contains one or more records).
     *
     * @param entityRows
     * @throws ServiceException
     */
    public void singleEntityBulkDeleteAndInsert(List<? extends MasterDataRegistry> entityRows) throws ServiceException {

        //deleteExistingFromIndex(entityRows.get(0).getClass());

        if (!CollectionUtils.isEmpty(entityRows)) {

            Class mdrClass  = entityRows.get(0).getClass();
            Session session = (getEntityManager().unwrap(Session.class)).getSessionFactory().openSession();
            Transaction tx  = session.beginTransaction();

            try {
                String entityName = mdrClass.getSimpleName();

                log.info("Persisting entity entries for : " + entityName);
                deleteAndPersistNewEntityByEntityName(entityRows, session, entityName);

                log.info("Rebuilding Lucene index for entity : " + entityName + "...");
                refreshLuceneIndexes(mdrClass);

                log.debug("Committing transaction.");
                getEntityManager().flush();
                tx.commit();

                log.info("Insertion for {} completed.", mdrClass.toString());
            } catch (Exception e) {
                tx.rollback();
                throw new ServiceException("Rollbacking transaction for reason : ", e);
            } finally {
                log.debug("Closing session");
                session.close();
            }
        }

    }

    private void deleteExistingFromIndex(Class<? extends MasterDataRegistry> aClass) throws ServiceException {

        Session session = (getEntityManager().unwrap(Session.class)).getSessionFactory().openSession();
        Transaction tx  = session.beginTransaction();
        try {
            QueryBuilder queryBuilder  =  Search.getFullTextEntityManager(em).getSearchFactory().buildQueryBuilder().forEntity(aClass).get();
            Query luceneQuery          = queryBuilder.keyword().wildcard().onField("code").matching("*").createQuery();
            IndexWriter indexWriter    = new IndexWriter(new RAMDirectory(), new IndexWriterConfig(Version.LUCENE_4_10_4, new StandardAnalyzer(Version.LATEST)));
            indexWriter.deleteDocuments(luceneQuery);
            tx.commit();
            log.info("Purging for {} completed.", aClass.toString());
        } catch (Exception e) {
            tx.rollback();
            throw new ServiceException("Rollbacking transaction for reason : ", e);
        } finally {
            log.debug("Closing session");
            session.close();
        }
    }

    /**
     * Flushes and commits all the changes.
     * Ps : Until this point nothing has yet been persisted to db.
     *
     * @param tx
     */
    private void flushAndCommitChanges(Transaction tx) {
        getEntityManager().flush();
        tx.commit();
    }

    /**
     * Refreshes the Lucene indexes with the latest deletions and insertions.
     *
     * @param mdrClass
     * @throws InterruptedException
     */
    private void refreshLuceneIndexes(Class mdrClass) throws InterruptedException {
        FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
        fullTextEntityManager.createIndexer(mdrClass).startAndWait();
    }

    /**
     * Massivly deletes all the previous indexes and depending on the annotated classes (entities)
     * it recreates the whole index (Ps : use with caution)
     *
     * @throws InterruptedException
     */
    private void massiveReIndex() throws InterruptedException {
        Search.getFullTextEntityManager(em).createIndexer().purgeAllOnStart(true).startAndWait();
    }

    private void deleteAndPersistNewEntityByEntityName(List<? extends MasterDataRegistry> entityRows, Session session,
                                                       String entityName) throws InterruptedException {
        // DELETION PHASE (Deleting old entries)
        session.createQuery(HQL_DELETE + entityName).executeUpdate();

        // INSERTION PHASE (Inserting new entries)
        for (MasterDataRegistry actualEnityRow : entityRows) {
            session.save(actualEnityRow);
        }
    }

    public MdrBulkOperationsDao(EntityManager em) {
        this.em = em;
    }

    public EntityManager getEntityManager() {
        return em;
    }
}