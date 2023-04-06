package org.healthnlp.deepphe.summary.attribute.cr.topo_minor.breast;

import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.summary.attribute.cr.topo_minor.AbstractTopoMinorInfoCollector;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @since {3/26/2023}
 */
public class BreastInfoCollector extends AbstractTopoMinorInfoCollector {


   public Collection<String> getRelationTypes() {
      return Arrays.asList( RelationConstants.HAS_CLOCKFACE, RelationConstants.HAS_QUADRANT );
   }

}
