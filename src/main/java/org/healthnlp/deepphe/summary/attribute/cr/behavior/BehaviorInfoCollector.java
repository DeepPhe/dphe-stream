package org.healthnlp.deepphe.summary.attribute.cr.behavior;

import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AbstractAttributeInfoCollector;

import java.util.Collection;
import java.util.Collections;

/**
 * @author SPF , chip-nlp
 * @since {3/23/2023}
 */
public class BehaviorInfoCollector extends AbstractAttributeInfoCollector {

   public Collection<String> getRelationTypes() {
      return Collections.singletonList( RelationConstants.HAS_BEHAVIOR );
   }



}
