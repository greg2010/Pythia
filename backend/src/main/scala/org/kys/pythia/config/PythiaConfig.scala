package org.kys.pythia.config

case class PythiaConfig(uwApi: String,
                        mailgunApi: String,
                        senderDomain: String,
                        senderEmail: String,
                        senderEmailName: String,
                        updatePeriodSeconds: Int)
