/*
 * Copyright 2012 SFB 632.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package annis;

import annis.dao.AnnisDao;
import annis.dao.SpringAnnisDao;
import annis.ql.parser.QueryData;
import annis.test.TestHelper;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeNoException;
import static org.junit.Assume.assumeTrue;;

import org.springframework.dao.DataAccessException;

/**
 * This will execute tests on a real database and check if the counts are OK.
 *
 * @author Thomas Krause <thomas.krause@alumni.hu-berlin.de>
 */
@RunWith(SpringJUnit4ClassRunner.class)
// TODO: do not test context only for annopool
@ContextConfiguration(locations =
{
  "file:src/main/distribution/conf/spring/Common.xml"
}, loader = AnnisXmlContextLoader.class)
public class CountTest
{

  @Resource(name="annisDao")
  AnnisDao annisDao;
  
  private List<Long> pcc2CorpusID;

  @Before
  public void setup()
  {
    SpringAnnisDao springAnnisDao = (SpringAnnisDao) TestHelper.proxyTarget(annisDao);
    
    try
    {
      assumeNotNull(springAnnisDao.getSimpleJdbcTemplate());
      
      springAnnisDao.getSimpleJdbcTemplate().queryForInt("SELECT 1");
      
    }
    catch (DataAccessException ex)
    {
      assumeNoException(ex);
    }
    
    // get the id of the "pcc2" corpus 
    // (and check if it's there, otherwise ignore these tests)
    List<String> corpusNames = new LinkedList<String>();
    corpusNames.add("pcc2");
    List<Long> corpusIDs = annisDao.mapCorpusNamesToIds(corpusNames);
    
    pcc2CorpusID = corpusIDs;
    
  }

  @Test
  public void testAQLTestSuitePcc2()
  {
    assumeTrue(pcc2CorpusID.size() > 0);
    
    assertEquals(5, countPcc2("Topic=\"ab\" & Inf-Stat=\"new\" & #1 _i_ #2"));
    assertEquals(5, countPcc2("np_form=\"defnp\" & np_form=\"pper\"  & #2 ->anaphor_antecedent  #1"));
    assertEquals(13, countPcc2("np_form=\"defnp\" & np_form=\"pper\"  & #2 ->anaphor_antecedent * #1"));
    assertEquals(2, countPcc2("np_form=\"defnp\" & np_form=\"pper\"  & #2 ->anaphor_antecedent * #1 & cat=\"NP\" & cat=\"S\" & #4 >[func=\"SB\"] #3 & #3 _i_ #2"));
    assertEquals(3, countPcc2("Inf-Stat=\"new\" & PP & #1 _o_ #2"));
    assertEquals(1, countPcc2("np_form=\"defnp\" & np_form=\"pper\"  & #2 ->anaphor_antecedent #1 & cat=\"NP\" & node & #4 >[func=\"OA\"] #3 & #3 _i_ #2"));
    
  }
  
  private int countPcc2(String aql)
  {
    QueryData qd = annisDao.parseAQL(aql, pcc2CorpusID);
    return annisDao.count(qd);
  }
}
