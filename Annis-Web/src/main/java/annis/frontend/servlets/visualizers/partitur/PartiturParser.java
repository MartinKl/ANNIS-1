/*
 * Copyright 2009 Collaborative Research Centre SFB 632 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package annis.frontend.servlets.visualizers.partitur;

import annis.model.AnnisNode;
import annis.model.Annotation;
import annis.model.AnnotationGraph;
import annis.model.Edge;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author thomas
 */
public class PartiturParser implements Serializable
{

  private List<Token> token;
  private Set<String> knownTiers;
  private HashMap<String, String> tier2ns;

  public PartiturParser(AnnotationGraph graph, String namespace)
  {
    token = new LinkedList<Token>();
    knownTiers = new HashSet<String>();

    tier2ns = new HashMap<String, String>();

    for (AnnisNode n : graph.getTokens())
    {
      long tokenID = n.getId();

      Token currentToken = new Token(tokenID, new Hashtable<String, Event>(), n.getSpannedText());

      token.add(currentToken);

      // get parent annotations matching namespace
      for(Edge edge : n.getIncomingEdges())
      {
        if(edge.getEdgeType() == Edge.EdgeType.COVERAGE)
        {
          AnnisNode parentNode = edge.getSource();
          for(Annotation anno : parentNode.getNodeAnnotations())
          {
            if(parentNode.getNamespace().equals(namespace))
            {
              // finally, put this annotation in the list
              Event newEvent = new Event(parentNode.getId(), anno.getValue());
              currentToken.getTier2Event().put(anno.getName(), newEvent);
              // update our set of tiers
              knownTiers.add(anno.getName());
              tier2ns.put(anno.getName(), anno.getNamespace());
            }
          }
        }
      }
    }

    // now connect the token to make it easier later to find the neighbors
    Iterator<Token> it = token.iterator();

    Token current = it.hasNext() ? it.next() : null;
    Token next = it.hasNext() ? it.next() : null;
    Token last = null;

    while (current != null)
    {
      current.setBefore(last);
      current.setAfter(next);

      last = current;
      current = next;
      next = it.hasNext() ? it.next() : null;
    }

  }

  public Set<String> getKnownTiers()
  {
    return knownTiers;
  }

  public String namespaceForTier(String tier)
  {
    return tier2ns.get(tier);
  }

  public List<Token> getToken()
  {
    return token;
  }

  public class Token implements Serializable
  {

    private Map<String, Event> tier2Event;
    private long id;
    private String value;
    private Token before;
    private Token after;

    public Token(long id, Map<String, Event> tier2Event, String value)
    {
      this.tier2Event = tier2Event;
      this.id = id;
      this.value = value;
      before = null;
      after = null;
    }

    public Map<String, Event> getTier2Event()
    {
      return tier2Event;
    }

    public long getId()
    {
      return id;
    }

    public Token getAfter()
    {
      return after;
    }

    public void setAfter(Token after)
    {
      this.after = after;
    }

    public Token getBefore()
    {
      return before;
    }

    public void setBefore(Token before)
    {
      this.before = before;
    }

    public String getValue()
    {
      return value;
    }
  }

  public class Event implements Serializable
  {

    private long id;
    private String value;

    public Event(long id, String value)
    {
      this.id = id;
      this.value = value;
    }

    public long getId()
    {
      return id;
    }

    public String getValue()
    {
      return value;
    }
  }
}