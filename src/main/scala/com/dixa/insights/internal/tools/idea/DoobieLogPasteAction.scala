// Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.dixa.insights.internal.tools.idea

import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent, CommonDataKeys}
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.ide.CopyPasteManager

import java.awt.datatransfer.DataFlavor
import java.io.StringWriter
import scala.util.Try

class DoobieLogPasteAction extends AnAction() {

  /**
   * Gives the user feedback when the dynamic action menu is chosen.
   * Pops a simple message dialog.
   * @param event Event received when the associated menu item is chosen.
   */
  override def actionPerformed(event: AnActionEvent): Unit = { // Using the event, create and show a dialog
    val currentProject = event.getProject

    val copyPasteManager = CopyPasteManager.getInstance()
    val stringFlavor = DataFlavor.stringFlavor
    val clipboardReader = stringFlavor.getReaderForText(copyPasteManager.getContents)
    val stringWriter = new StringWriter()
    clipboardReader.transferTo(stringWriter)
    val clipboardString = stringWriter.toString
    println(clipboardString)
    val parsedSqlSnippetOpt = interpolateFromLog(clipboardString)
    parsedSqlSnippetOpt match {
      case None => println("failed to parse")
      case Some(sqlSnippet) =>
        println(sqlSnippet)
        val editor = event.getRequiredData(CommonDataKeys.EDITOR)
        val caret = editor.getCaretModel.getPrimaryCaret
        val document = editor.getDocument
        WriteCommandAction.runWriteCommandAction(currentProject, new Runnable {
          override def run(): Unit = document.insertString(caret.getOffset, sqlSnippet)
        })

    }
  }

  /**
   * Determines whether this menu item is available for the current context.
   * Requires a project to be open.
   *
   * @param e Event received when the associated group-id menu is chosen.
   */
  override def update(e: AnActionEvent): Unit = { // Set the availability based on whether a project is open
    val project = e.getProject
    e.getPresentation.setEnabledAndVisible(project != null)
  }

  private def interpolateParams(sql: String, params: List[String]): String = params match {
    case p::ps =>
      val isNumber = Try(p.toDouble).isSuccess
      val paramStr = if (isNumber) {
        p
      } else {
        s"'$p'"
      }
      val newSql = sql.replaceFirst("""\?""", paramStr)
      interpolateParams(newSql, ps)
    case Nil => sql
  }

  private def interpolateFromLog(logLines: String): Option[String] = {
    val contentLines = logLines.split('\n').map(_.trim).filterNot(_.isEmpty)
    val selectStmtOpt = contentLines.find(_.startsWith("SELECT"))
    val argsOpt = contentLines.find(_.startsWith("arguments")).map { argsLine =>
      val argsStr = argsLine.replace("arguments = [", "").replace("]", "")
      argsStr.split(", ").toList
    }
    (selectStmtOpt, argsOpt) match {
      case (Some(selectStmt), Some(args)) =>
        Some(interpolateParams(selectStmt, args))
      case _ => None
    }
  }
}
