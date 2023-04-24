package org.healthnlp.deepphe.summary.attribute.cr.topo_minor;

import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AbstractAttributeInfoCollector;

import java.util.Collection;
import java.util.Collections;

/**
 * @author SPF , chip-nlp
 * @since {3/26/2023}
 */
abstract public class AbstractTopoMinorInfoCollector extends AbstractAttributeInfoCollector {


   public Collection<String> getRelationTypes() {
      return Collections.singletonList( RelationConstants.DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE );
   }


}
