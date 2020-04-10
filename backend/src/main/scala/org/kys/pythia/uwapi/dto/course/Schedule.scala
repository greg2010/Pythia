package org.kys.pythia.uwapi.dto.course

case class Schedule(subject: String,
                    catalog_number: String,
                    title: String,
                    class_number: Int,
                    section: String,
                    enrollment_capacity: Int,
                    enrollment_total: Int,
                    waiting_capacity: Int,
                    waiting_total: Int)