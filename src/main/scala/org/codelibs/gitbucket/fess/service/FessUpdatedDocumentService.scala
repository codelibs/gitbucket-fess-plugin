package org.codelibs.gitbucket.fess.service

import gitbucket.core.util.Directory._
import gitbucket.core.util.SyntaxSugars._
import gitbucket.core.util.JGitUtil
import java.io.File

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.{ReceiveCommand, ReceivePack}

import scala.io.Source

trait FessUpdatedDocumentService {

  val UpdatedFile = new File(GitBucketHome, "fess_updated.csv")

  def reset(): Unit =
    using(new java.io.FileOutputStream(UpdatedFile)) { _.write("".getBytes) }

  def load(): List[UpdatedDoc] = {
    if (!UpdatedFile.exists) {
      return List()
    }
    using(Source.fromFile(UpdatedFile)) { src =>
      src
        .getLines()
        .flatMap(_.split(',') match {
          case Array(owner, repo, path) => Some(UpdatedDoc(owner, repo, path))
          case _                        => None
        })
        .toList
    }
  }

  def logging(owner: String,
              repository: String,
              receivePack: ReceivePack,
              command: ReceiveCommand,
              pusher: String): Unit =
    using(Git.open(getRepositoryDir(owner, repository))) { git =>
      val diff = JGitUtil.getDiffs(git,
                                   command.getOldId.getName,
                                   command.getNewId.getName,
                                   false)
      val changedFiles = diff
        .flatMap(d => List(d.oldPath, d.newPath))
        .distinct
        .map(f => UpdatedDoc(owner, repository, f))
      val loadedFiles  = load()
      val updatedFiles = (changedFiles ::: loadedFiles).sorted.distinct
      using(new java.io.FileOutputStream(UpdatedFile)) { out =>
        updatedFiles.foreach(doc => {
          if (!doc.path
                .eq("/dev/null")) { // FIXME: Where does "/dev/null" come from?
            out.write(s"${doc.owner},${doc.repo},${doc.path}\n".getBytes)
          }
        })
      }
    }
}

case class UpdatedDoc(owner: String, repo: String, path: String)
    extends Ordered[UpdatedDoc] {
  import scala.math.Ordered.orderingToOrdered
  def compare(that: UpdatedDoc): Int =
    (this.owner, this.repo, this.path) compare (that.owner, that.repo, that.path)
}
