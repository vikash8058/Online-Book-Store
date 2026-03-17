package com.bookstore.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

	private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

	// BOOK SERVICE — createBook method

	@Pointcut("execution(* com.bookstore.service.BookService.createBook(..))")
	public void createBookMethod() {
	}

	@Before("createBookMethod()")
	public void beforeCreateBook(JoinPoint joinPoint) {
		log.info("==============================================");
		log.info("BOOK SERVICE - METHOD STARTED : createBook");
		log.info("==============================================");
	}

	@AfterReturning(pointcut = "createBookMethod()", returning = "result")
	public void afterCreateBook(JoinPoint joinPoint, Object result) {
		log.info("BOOK SERVICE - METHOD SUCCESS : createBook");
		log.info("BOOK CREATED  : {}", result);
	}

	@AfterThrowing(pointcut = "createBookMethod()", throwing = "exception")
	public void afterCreateBookException(JoinPoint joinPoint, Throwable exception) {
		log.error("BOOK SERVICE - METHOD FAILED : createBook");
		log.error("EXCEPTION     : {}", exception.getMessage());
	}

	// USER SERVICE — registerUser method

	@Pointcut("execution(* com.bookstore.service.UserService.registerUser(..))")
	public void registerUserMethod() {
	}

	@Before("registerUserMethod()")
	public void beforeRegisterUser(JoinPoint joinPoint) {
		log.info("==============================================");
		log.info("USER SERVICE - METHOD STARTED : registerUser");
		log.info("==============================================");
	}

	@AfterReturning(pointcut = "registerUserMethod()", returning = "result")
	public void afterRegisterUser(JoinPoint joinPoint, Object result) {
		log.info("USER SERVICE - METHOD SUCCESS : registerUser");
		log.info("USER CREATED  : {}", result);
	}

	@AfterThrowing(pointcut = "registerUserMethod()", throwing = "exception")
	public void afterRegisterUserException(JoinPoint joinPoint, Throwable exception) {
		log.error("USER SERVICE - METHOD FAILED : registerUser");
		log.error("EXCEPTION     : {}", exception.getMessage());
	}

	// ORDER SERVICE — createOrder method

	@Pointcut("execution(* com.bookstore.service.OrderService.createOrder(..))")
	public void createOrderMethod() {
	}

	@Before("createOrderMethod()")
	public void beforeCreateOrder(JoinPoint joinPoint) {
		log.info("==============================================");
		log.info("ORDER SERVICE - METHOD STARTED : createOrder");
		log.info("==============================================");
	}

	@AfterReturning(pointcut = "createOrderMethod()", returning = "result")
	public void afterCreateOrder(JoinPoint joinPoint, Object result) {
		log.info("ORDER SERVICE - METHOD SUCCESS : createOrder");
		log.info("ORDER CREATED : {}", result);
	}

	@AfterThrowing(pointcut = "createOrderMethod()", throwing = "exception")
	public void afterCreateOrderException(JoinPoint joinPoint, Throwable exception) {
		log.error("ORDER SERVICE - METHOD FAILED : createOrder");
		log.error("EXCEPTION     : {}", exception.getMessage());
	}
}