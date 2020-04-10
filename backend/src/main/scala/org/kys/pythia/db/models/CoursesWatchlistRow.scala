package org.kys.pythia.db.models

case class CoursesWatchlistRow(id: Long,
                               sectionId: Int,
                               termId: Int,
                               email: String,
                               lastStateHasSpace: Boolean,
                               friendlyName: String)
