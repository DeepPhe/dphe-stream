package org.apache.ctakes.core.store;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/30/2020
 */
public class CreatingCleaningStore<T> extends SelfCleaningStore<T> implements CreatingObjectStore<T> {

   public CreatingCleaningStore( final ObjectStore<T> delegate ) {
      super( delegate );
   }

}
