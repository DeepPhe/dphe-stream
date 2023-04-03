package org.healthnlp.deepphe.summary.attribute.cr.tnm;

import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AbstractAttributeInfoCollector;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @since {3/24/2023}
 */
public class N_InfoCollector extends AbstractAttributeInfoCollector {


   public Collection<String> getRelationTypes() {
      return Arrays.asList( RelationConstants.HAS_CLINICAL_N, RelationConstants.HAS_PATHOLOGIC_N );
   }

}
