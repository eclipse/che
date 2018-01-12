/*
 * Copyright (c) 2016-2018 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc.- initial API and implementation
 */
/**
 * Interface required to be implemented to subscribe to message bus messages.
 * @author Florent Benoit
 */
export interface MessageBusSubscriber {

    handleMessage(message: string);


}
