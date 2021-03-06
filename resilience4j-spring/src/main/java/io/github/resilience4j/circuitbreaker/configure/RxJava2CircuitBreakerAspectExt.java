/*
 * Copyright 2019 Mahmoud Romeh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.circuitbreaker.configure;

import static io.github.resilience4j.utils.AspectUtil.newHashSet;

import java.util.Set;

import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.operator.CircuitBreakerOperator;
import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.SingleSource;

/**
 * the Rx circuit breaker logic support for the spring AOP
 * conditional on the presence of Rx classes on the spring class loader
 */
public class RxJava2CircuitBreakerAspectExt implements CircuitBreakerAspectExt {

	private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerAspect.class);
	private final Set<Class> rxSupportedTypes = newHashSet(ObservableSource.class, SingleSource.class, CompletableSource.class, MaybeSource.class, Flowable.class);

	/**
	 * @param returnType the AOP method return type class
	 * @return boolean if the method has Rx java 2 rerun type
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean canHandleReturnType(Class returnType) {
		return rxSupportedTypes.stream().anyMatch(classType -> classType.isAssignableFrom(returnType));
	}

	/**
	 * @param proceedingJoinPoint Spring AOP proceedingJoinPoint
	 * @param circuitBreaker      the configured circuitBreaker
	 * @param methodName          the method name
	 * @return the result object
	 * @throws Throwable exception in case of faulty flow
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Object handle(ProceedingJoinPoint proceedingJoinPoint, CircuitBreaker circuitBreaker, String methodName) throws Throwable {
		CircuitBreakerOperator circuitBreakerOperator = CircuitBreakerOperator.of(circuitBreaker);
		Object returnValue = proceedingJoinPoint.proceed();
		if (returnValue instanceof ObservableSource) {
			Observable<?> observable = (Observable) returnValue;
			return observable.lift(circuitBreakerOperator);
		} else if (returnValue instanceof SingleSource) {
			Single<?> single = (Single) returnValue;
			return single.lift(circuitBreakerOperator);
		} else if (returnValue instanceof CompletableSource) {
			Completable completable = (Completable) returnValue;
			return completable.lift(circuitBreakerOperator);
		} else if (returnValue instanceof MaybeSource) {
			Maybe<?> maybe = (Maybe) returnValue;
			return maybe.lift(circuitBreakerOperator);
		} else if (returnValue instanceof Flowable) {
			Flowable<?> flowable = (Flowable) returnValue;
			return flowable.lift(circuitBreakerOperator);
		} else {
			logger.error("Unsupported type for RxJava2 circuit breaker return type {} for method {}", returnValue.getClass().getTypeName(), methodName);
			throw new IllegalArgumentException("Not Supported type for the circuit breaker in RxJava2:" + returnValue.getClass().getName());
		}
	}
}
