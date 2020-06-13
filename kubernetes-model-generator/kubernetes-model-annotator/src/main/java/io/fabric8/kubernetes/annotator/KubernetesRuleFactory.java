package io.fabric8.kubernetes.annotator;

import java.lang.annotation.Annotation;

import org.jsonschema2pojo.Schema;
import org.jsonschema2pojo.rules.ObjectRule;
import org.jsonschema2pojo.rules.Rule;
import org.jsonschema2pojo.rules.RuleFactory;
import org.jsonschema2pojo.util.ParcelableHelper;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpressionImpl;
import com.sun.codemodel.JFormatter;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JType;

public class KubernetesRuleFactory extends RuleFactory {

  @Override
  public Rule<JPackage, JType> getObjectRule() {
    return new ObjectRule(this, new ParcelableHelper()) {
      @Override
      public JType apply(String nodeName, JsonNode node, JPackage _package, Schema schema) {
        JDefinedClass clazz = (JDefinedClass) super.apply(nodeName, node, _package, schema);

        if (node.has("serializer")) {
          annotateSerde(clazz, JsonSerialize.class, node.get("serializer").asText());
        }

        if (node.has("deserializer")) {
          annotateSerde(clazz, JsonDeserialize.class, node.get("deserializer").asText());
        } else {
          clazz.annotate(JsonDeserialize.class).param("using", JsonDeserializer.None.class);
        }

        return clazz;
      }

      private void annotateSerde(JDefinedClass clazz, Class<? extends Annotation> annotation, String usingClassName) {
        if (!usingClassName.endsWith(".class")) {
          usingClassName = usingClassName + ".class";
        }

        clazz.annotate(annotation).param("using", literalExpression(usingClassName));
      }

      private JExpressionImpl literalExpression(String literal) {
        return new JExpressionImpl() {
          @Override
          public void generate(JFormatter f) {
            f.p(literal);
          }
        };
      }

    };
  }
}
