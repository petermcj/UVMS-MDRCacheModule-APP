package eu.europa.ec.fisheries.mdr.bean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import eu.europa.ec.fisheries.mdr.dao.BaseMdrDaoTest;
import eu.europa.ec.fisheries.mdr.entities.codelists.ers.FaoSpecies;
import eu.europa.ec.fisheries.mdr.repository.bean.MdrLuceneSearchRepositoryBean;
import eu.europa.ec.fisheries.mdr.repository.bean.MdrRepositoryBean;
import eu.europa.ec.fisheries.uvms.exception.ServiceException;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

/**
 * Created by georgige on 11/15/2016.
 */
public class MdrLuceneSearchRepositoryBeanTest extends BaseMdrDaoTest {

    private MdrRepositoryBean mdrInsertionRepoBean             = new MdrRepositoryBean();
    private MdrLuceneSearchRepositoryBean mdrSearchingRepoBean = new MdrLuceneSearchRepositoryBean();

    public static final String CODE = "code";

    @Before
    @SneakyThrows
    public void prepare() {
        org.hibernate.search.jpa.Search.getFullTextEntityManager(em).flushToIndexes();
        Whitebox.setInternalState(mdrSearchingRepoBean, "postgres", em);
        Whitebox.setInternalState(mdrInsertionRepoBean, "postgres", em);
        mdrSearchingRepoBean.init();
        mdrInsertionRepoBean.init();
        mdrInsertionRepoBean.insertNewData(mockSpecies());
    }

    @Test
    @SneakyThrows
    public void testLuceneIndexingNoSearchFilters() throws ServiceException {
        List<FaoSpecies> species = mockSpecies();

        FullTextSession fullTextSession = Search.getFullTextSession((Session) em.getDelegate());
        Transaction tx = fullTextSession.beginTransaction();
        FaoSpecies faoSpecies = (FaoSpecies) fullTextSession.load( FaoSpecies.class, 1L );
        fullTextSession.index(faoSpecies);
        tx.commit(); //index only updated at commit time

        try {
            mdrSearchingRepoBean.findCodeListItemsByAcronymAndFilter(species.get(0).getAcronym(), 0, 5, CODE, false, null, null);
            fail("ServiceException was expected but not thrown.");
        } catch (Exception exc) {
            assertTrue(exc.getCause() instanceof IllegalArgumentException);
            assertEquals("No search attributes are provided.", exc.getMessage());
        }

    }


    @Test
    @SneakyThrows
    public void testLuceneSearch() throws ServiceException {
        List<FaoSpecies> species = mockSpecies();

        List<FaoSpecies> filterredEntities = (List<FaoSpecies>) mdrSearchingRepoBean.findCodeListItemsByAcronymAndFilter(species.get(0).getAcronym(),
                0, 5, CODE, true, "*", CODE);

        assertEquals(3, filterredEntities.size());
        assertEquals("WHL", filterredEntities.get(0).getCode());
        assertEquals("COD", filterredEntities.get(1).getCode());
        assertEquals("CAT", filterredEntities.get(2).getCode());
    }

    @Test
    @SneakyThrows
    public void testLuceneSearchOnMultipleFields() throws ServiceException {
        List<FaoSpecies> species = mockSpecies();

        String[] fields= {CODE, "description"};
        final String filterText = "*whl";
        final String filterText_2 = "c*";

        List<FaoSpecies> filterredEntities = (List<FaoSpecies>) mdrSearchingRepoBean.findCodeListItemsByAcronymAndFilter(species.get(0).getAcronym(),
                0, 5, CODE, true, filterText, fields);

        List<FaoSpecies> filterredEntities_2 = (List<FaoSpecies>) mdrSearchingRepoBean.findCodeListItemsByAcronymAndFilter(species.get(0).getAcronym(),
                0, 5, CODE, true, filterText_2, fields);

        assertEquals(1, filterredEntities.size());
        assertEquals("WHL", filterredEntities.get(0).getCode());

        assertEquals(2, filterredEntities_2.size());
    }

    @Test
    @SneakyThrows
    public void testLuceneSearchCount() throws ServiceException {
        List<FaoSpecies> species = mockSpecies();
        int totalCount=  mdrSearchingRepoBean.countCodeListItemsByAcronymAndFilter(species.get(0).getAcronym(), "c*", CODE);
        assertEquals(2, totalCount);
    }

    private List<FaoSpecies> mockSpecies() {
        List<FaoSpecies> species = new ArrayList<>(2);
        FaoSpecies species1 = new FaoSpecies();
        species1.setCode("COD");
        species1.setEnName("COD fish");
        FaoSpecies species2 = new FaoSpecies();
        species2.setCode("CAT");
        species2.setEnName("CAT fish");
        FaoSpecies species3 = new FaoSpecies();
        species3.setCode("WHL");
        species3.setEnName("Whale");
        species.add(species1);
        species.add(species2);
        species.add(species3);
        return species;
    }

}
