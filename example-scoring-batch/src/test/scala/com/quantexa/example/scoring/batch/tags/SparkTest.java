package com.quantexa.example.scoring.batch.tags;

import java.lang.annotation.*;
import org.scalatest.TagAnnotation;

@Inherited
@TagAnnotation
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface SparkTest {}