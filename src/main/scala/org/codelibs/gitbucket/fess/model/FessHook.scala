package org.codelibs.gitbucket.fess.model

import gitbucket.core.model.Session
import gitbucket.core.plugin.ReceiveHook
import org.codelibs.gitbucket.fess.service.FessUpdatedDocumentService
import org.eclipse.jgit.transport.{ReceiveCommand, ReceivePack}

object FessHook extends ReceiveHook with FessUpdatedDocumentService {

  // FIXME: When files are edited on browers, this function is not called
  override def postReceive(owner: String,
                           repository: String,
                           receivePack: ReceivePack,
                           command: ReceiveCommand,
                           pusher: String)(implicit session: Session): Unit =
    logging(owner, repository, receivePack, command, pusher)
}
