package org.kys.pythia.config

case class DatabaseConfig(driver: String, url: String, user: String, password: String, threadPoolSize: Int)
