package checkers.regex;

import java.util.regex.Pattern;

import javax.lang.model.element.AnnotationMirror;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.PolyAll;
import checkers.quals.TypeQualifiers;
import checkers.quals.Unqualified;
import checkers.regex.quals.PartialRegex;
import checkers.regex.quals.PolyRegex;
import checkers.regex.quals.Regex;
import checkers.regex.quals.RegexBottom;
import checkers.types.QualifierHierarchy;
import checkers.util.AnnotationUtils;
import checkers.util.GraphQualifierHierarchy;
import checkers.util.MultiGraphQualifierHierarchy.MultiGraphFactory;

/**
 * A type-checker plug-in for the {@link Regex} qualifier that finds
 * syntactically invalid regular expressions.
 */
@TypeQualifiers({ Regex.class, PartialRegex.class, RegexBottom.class,
    Unqualified.class, PolyRegex.class, PolyAll.class })
public class RegexChecker extends BaseTypeChecker {

    protected AnnotationMirror REGEX, PARTIALREGEX;
    // TODO use? private TypeMirror[] legalReferenceTypes;

    @Override
    public void initChecker() {
        super.initChecker();

        AnnotationUtils annoFactory = AnnotationUtils.getInstance(processingEnv);
        REGEX = annoFactory.fromClass(Regex.class);
        PARTIALREGEX = annoFactory.fromClass(PartialRegex.class);

        /*
        legalReferenceTypes = new TypeMirror[] {
            getTypeMirror("java.lang.CharSequence"),
            getTypeMirror("java.lang.Character"),
            getTypeMirror("java.util.regex.Pattern"),
            getTypeMirror("java.util.regex.MatchResult") };
         */
    }

    /**
     * Gets a TypeMirror for the given class name.
    private TypeMirror getTypeMirror(String className) {
        return processingEnv.getElementUtils().getTypeElement(className).asType();
    }
    */

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new RegexQualifierHierarchy(factory, AnnotationUtils.getInstance(processingEnv).fromClass(RegexBottom.class));
    }

    /**
     * A custom qualifier hierarchy for the Regex Checker. This makes a regex
     * annotation a subtype of all regex annotations with lower group count
     * values. For example, {@code @Regex(3)} is a subtype of {@code @Regex(1)}.
     * All regex annotations are subtypes of {@code @Regex} which has a default
     * value of 0.
     */
    private final class RegexQualifierHierarchy extends GraphQualifierHierarchy {

        public RegexQualifierHierarchy(MultiGraphFactory f,
                AnnotationMirror bottom) {
            super(f, bottom);
        }

        @Override
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
            if (AnnotationUtils.areSameIgnoringValues(rhs, REGEX)
                    && AnnotationUtils.areSameIgnoringValues(lhs, REGEX)) {
                int rhsValue = getRegexValue(rhs);
                int lhsValue = getRegexValue(lhs);
                return lhsValue <= rhsValue;
            }
            // TODO: subtyping between PartialRegex?
            // Ignore annotation values to ensure that annotation is in supertype map.
            if (AnnotationUtils.areSameIgnoringValues(lhs, REGEX)) {
                lhs = REGEX;
            }
            if (AnnotationUtils.areSameIgnoringValues(rhs, REGEX)) {
                rhs = REGEX;
            }
            if (AnnotationUtils.areSameIgnoringValues(lhs, PARTIALREGEX)) {
                lhs = PARTIALREGEX;
            }
            if (AnnotationUtils.areSameIgnoringValues(rhs, PARTIALREGEX)) {
                rhs = PARTIALREGEX;
            }
            return super.isSubtype(rhs, lhs);
        }

        /**
         * Gets the value out of a regex annotation.
         */
        private int getRegexValue(AnnotationMirror anno) {
            return AnnotationUtils.elementValueWithDefaults(anno, "value", Integer.class);
        }
    }

    /**
     * Returns the group count value of the given annotation or 0 if
     * there's a problem getting the group count value.
     */
    public int getGroupCount(AnnotationMirror anno) {
        // If group count value is null then there's no Regex annotation
        // on the parameter so set the group count to 0. This would happen
        // if a non-regex string is passed to Pattern.compile but warnings
        // are suppressed.
        return anno == null ? 0 : AnnotationUtils.elementValueWithDefaults(anno, "value", Integer.class);
    }

    /**
     * Returns the number of groups in the given regex String.
     */
    public int getGroupCount(/*@Regex*/ String regex) {
        return Pattern.compile(regex).matcher("").groupCount();
    }
}
