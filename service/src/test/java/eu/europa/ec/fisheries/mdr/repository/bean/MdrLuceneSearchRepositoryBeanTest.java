package eu.europa.ec.fisheries.mdr.repository.bean;

import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import com.ninja_squad.dbsetup.operation.Operation;
import eu.europa.ec.fisheries.mdr.dao.BaseMdrDaoTest;
import eu.europa.ec.fisheries.mdr.domain.codelists.FaoSpecies;
import eu.europa.ec.fisheries.uvms.exception.ServiceException;
import lombok.SneakyThrows;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

import java.util.ArrayList;
import java.util.List;

import static com.ninja_squad.dbsetup.Operations.sequenceOf;
import static org.junit.Assert.*;

/**
 * Created by georgige on 11/15/2016.
 */
@Ignore
public class MdrLuceneSearchRepositoryBeanTest extends BaseMdrDaoTest {

    private MdrRepositoryBean mdrInsertionRepoBean = new MdrRepositoryBean();

    private MdrLuceneSearchRepositoryBean mdrRepoBean = new MdrLuceneSearchRepositoryBean();

    public static final String CODE = "code";

    @Before
    @SneakyThrows
    public void prepare() {
        Operation operation = sequenceOf(DELETE_ALL_MDR_SPECIES);
        DbSetup dbSetup = new DbSetup(new DataSourceDestination(ds), operation);
        dbSetupTracker.launchIfNecessary(dbSetup);
        //init the beans
        Whitebox.setInternalState(mdrRepoBean, "em", em);
        Whitebox.setInternalState(mdrInsertionRepoBean, "em", em);
        mdrRepoBean.init();
        mdrInsertionRepoBean.init();
    }

    @Test
    @SneakyThrows
    public void testLuceneIndexingNoSearchFilters() throws ServiceException {
        List<FaoSpecies> species = mockSpecies();

        mdrInsertionRepoBean.insertNewData(species);

        FullTextSession fullTextSession = Search.getFullTextSession((Session) em.getDelegate());
        Transaction tx = fullTextSession.beginTransaction();
        FaoSpecies faoSpecies = (FaoSpecies) fullTextSession.load( FaoSpecies.class, 1L );
        fullTextSession.index(faoSpecies);
        tx.commit(); //index only updated at commit time

        try {
            mdrRepoBean.findCodeListItemsByAcronymAndFilter(species.get(0).getAcronym(), 0, 5, CODE, false, null, null);
            fail("ServiceException was expected but not thrown.");
        } catch (Exception exc) {
            assertTrue(exc instanceof  IllegalArgumentException);
            assertEquals("No search attributes are provided.", exc.getMessage());
        }

    }


    @Test
    @SneakyThrows
    public void testLuceneSearch() throws ServiceException {
        List<FaoSpecies> species = mockSpecies();
        mdrInsertionRepoBean.insertNewData(species);

        List<FaoSpecies> filterredEntities = (List<FaoSpecies>) mdrRepoBean.findCodeListItemsByAcronymAndFilter(species.get(0).getAcronym(),
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
        mdrInsertionRepoBean.insertNewData(species);

        String[] fields= {CODE, "description"};
        final String filterText = "*whl";
        final String filterText_2 = "c*";

        List<FaoSpecies> filterredEntities = (List<FaoSpecies>) mdrRepoBean.findCodeListItemsByAcronymAndFilter(species.get(0).getAcronym(),
                0, 5, CODE, true, filterText, fields);

        List<FaoSpecies> filterredEntities_2 = (List<FaoSpecies>) mdrRepoBean.findCodeListItemsByAcronymAndFilter(species.get(0).getAcronym(),
                0, 5, CODE, true, filterText_2, fields);

        assertEquals(1, filterredEntities.size());
        assertEquals("WHL", filterredEntities.get(0).getCode());

        assertEquals(2, filterredEntities_2.size());
    }

    @Test
    @SneakyThrows
    public void testLuceneSearchCount() throws ServiceException {
        List<FaoSpecies> species = mockSpecies();
        mdrInsertionRepoBean.insertNewData(species);

        int totalCount=  mdrRepoBean.countCodeListItemsByAcronymAndFilter(species.get(0).getAcronym(), "c*", CODE);
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
