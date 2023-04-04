package org.healthnlp.deepphe.summary.attribute.cr.laterality;

import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AbstractAttributeInfoCollector;

import java.util.Collection;
import java.util.Collections;

/**
 * @author SPF , chip-nlp
 * @since {3/24/2023}
 */
public class LateralityInfoCollector extends AbstractAttributeInfoCollector {

   public Collection<String> getRelationTypes() {
      return Collections.singletonList( RelationConstants.HAS_LATERALITY );
   }


}