package de.digitalstep.timetrack.persistence

import java.time._

import com.typesafe.scalalogging.LazyLogging
import org.parboiled2.ParserInput

import scala.collection.mutable
import scala.io.Source.fromInputStream
import scala.language.implicitConversions
import scala.reflect.io.Path.string2path
import scala.reflect.io.{File, Path}

private[persistence] object TextStorage {
  private[this] implicit def path2string(path: Path): ParserInput = fromInputStream(File(path).inputStream()).mkString

  private[this] val path = sys.props("user.home") / ".digitalstep" / "Zeiterfassung.txt"

  def apply(): TextStorage = apply(path)

  def apply(path: Path): TextStorage = new TextStorage(
    () ⇒ InputParser(path).days,
    Serializer(path))

}

private[persistence] class TextStorage(
                                        input: () ⇒ Iterable[Day],
                                        output: Serializer)
  extends Storage with LazyLogging {

  val dayMap: mutable.Map[LocalDate, Seq[Task]] = mutable.Map() ++
    input().map(d ⇒ d.date → d.tasks)

  def days: Iterable[Day] = for ((date, tasks) ← dayMap) yield Day(date, tasks)

  def findDay(date: LocalDate): Option[Day] = dayMap.get(date).map(t ⇒ Day(date, t))

  def save(): Unit = {
    logger.debug("Saving to {}", output)
    InputText(dayMap.toSeq.map(entry ⇒ Day(entry._1, entry._2)))
    output.serialize(
      InputText(
        for ((date, tasks) ← dayMap) yield Day(date, tasks)
      )
    )

    dayMap.clear()
    dayMap ++= input().map(d ⇒ d.date → d.tasks)
  }

  def add(date: LocalDate, task: Task): Storage = {
    dayMap.put(date, task :: dayMap.getOrElse(date, Seq()).toList)
    logger.debug("Storage now holds {} sections", new Integer(dayMap.keys.size))
    this
  }
}