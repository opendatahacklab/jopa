package cz.cvut.kbss.owlpersistence.model.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cz.cvut.kbss.owlpersistence.model.SequencesVocabulary;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Sequence {

	/**
	 * Defines the type of the sequence.
	 */
	SequenceType type() default SequenceType.referenced;

	/**
	 * URI of the class that represents the 'OWLList' concept.
	 * 
	 * Relevant only for REFERENCED type.
	 */
	String ClassOWLListIRI() default SequencesVocabulary.s_c_OWLList;
	
	/**
	 * URI of the object property that represents the 'hasContents' role.
	 * 
	 * Relevant only for REFERENCED type.
	 */
	String ObjectPropertyHasContentsIRI() default SequencesVocabulary.s_p_hasContents;

	/**
	 * URI of the object property that represents the 'hasNext' role.
	 */
	String ObjectPropertyHasNextIRI() default SequencesVocabulary.s_p_hasNext;
}