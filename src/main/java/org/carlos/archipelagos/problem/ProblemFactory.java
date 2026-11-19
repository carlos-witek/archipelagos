package org.carlos.archipelagos.problem;

import java.util.List;

import org.carlos.archipelagos.param.ParamSpec;
import org.carlos.archipelagos.param.Params;

/**
 * Spring-discovered provider of one problem (successor of the 2008 {@code ProblemProvider},
 * which was found by scanning JAR entries in a class loader and shipped to servers over RMI).
 */
public interface ProblemFactory {

    String name();

    String description();

    List<ParamSpec> params();

    Problem<?> create(Params params);
}
