/*******************************************************************************
 * Copyright (c) 2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Zend Technologies - initial API and implementation
 *******************************************************************************/
package zend.com.che.plugin.zdb.server.variables;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.che.api.debug.shared.model.Variable;
import org.eclipse.che.api.debug.shared.model.VariablePath;
import org.eclipse.che.api.debug.shared.model.impl.VariablePathImpl;

import zend.com.che.plugin.zdb.server.connection.IDebugExpression;
import zend.com.che.plugin.zdb.server.connection.IDebugExpressionValue;
import zend.com.che.plugin.zdb.server.connection.ZendDebugExpressionResolver;

/**
 * Debug Zend variable.
 * 
 * @author Bartlomiej Laczkowski
 */
public class ZendDebugVariable implements Variable {
	private final IDebugExpression expression;
	private final ZendDebugExpressionResolver expressionResolver;
	private String name;
	private boolean isExistInformation;
	private String value;
	private String type;
	private boolean isPrimitive;
	private boolean hasVariables;
	private List<Variable> variables;
	private VariablePath variablePath;

	public ZendDebugVariable(VariablePath variablePath, IDebugExpression expression, ZendDebugExpressionResolver expressionResolver) {
		this.variablePath = variablePath;
		this.expression = expression;
		this.expressionResolver = expressionResolver;
		create();
	}

	private void create() {
		IDebugExpressionValue expressionValue = expression.getValue();
		this.name = expression.getName();
		this.value = expressionValue.getValue();
		this.type = expressionValue.getDataType().getText();
		this.hasVariables = expressionValue.getChildrenCount() > 0;
		this.variablePath = new VariablePathImpl(name);
		this.isExistInformation = true;
		switch (expressionValue.getDataType()) {
		case PHP_BOOL:
		case PHP_FLOAT:
		case PHP_INT:
		case PHP_STRING:
		case PHP_NULL:
		case PHP_UNINITIALIZED: {
			this.isPrimitive = true;
			break;
		}
		default:
			this.isPrimitive = false;
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isExistInformation() {
		return isExistInformation;
	}

	@Override
	public String getValue() {
		return value;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public boolean isPrimitive() {
		return isPrimitive;
	}

	@Override
	public List<Variable> getVariables() {
		if (variables == null) {
			if (!hasVariables) {
				variables = Collections.unmodifiableList(Collections.emptyList());
			} else {
				variables = Collections.unmodifiableList(fetchVariables());
			}
		}
		return variables;
	}

	@Override
	public VariablePath getVariablePath() {
		return variablePath;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof ZendDebugVariable))
			return false;

		ZendDebugVariable variable = (ZendDebugVariable) o;

		if (isExistInformation != variable.isExistInformation)
			return false;
		if (isPrimitive != variable.isPrimitive)
			return false;
		if (name != null ? !name.equals(variable.name) : variable.name != null)
			return false;
		if (value != null ? !value.equals(variable.value) : variable.value != null)
			return false;
		if (type != null ? !type.equals(variable.type) : variable.type != null)
			return false;
		if (variables != null ? !variables.equals(variable.variables) : variable.variables != null)
			return false;
		return !(variablePath != null ? !variablePath.equals(variable.variablePath) : variable.variablePath != null);
	}

	@Override
	public int hashCode() {
		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + (isExistInformation ? 1 : 0);
		result = 31 * result + (value != null ? value.hashCode() : 0);
		result = 31 * result + (type != null ? type.hashCode() : 0);
		result = 31 * result + (isPrimitive ? 1 : 0);
		result = 31 * result + (variables != null ? variables.hashCode() : 0);
		result = 31 * result + (variablePath != null ? variablePath.hashCode() : 0);
		return result;
	}

	private List<Variable> fetchVariables() {
		List<Variable> children = new ArrayList<>();
		expressionResolver.resolve(expression, 1);
		for (IDebugExpression child : expression.getValue().getChildren()) {
			List<String> childPath = new ArrayList<>(variablePath.getPath());
			childPath.add(child.getName());
			children.add(new ZendDebugVariable(new VariablePathImpl(childPath), child, expressionResolver));
		}
		return children;
	}

}
