package services

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import javax.inject._
import actors.{RankActor, LoggerActor, IndexatorActor, CrawlTasksCreatorActor}
import akka.actor.{Props, ActorSystem}
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import protocols.CrawlQueueCreatorActorProtocol.StartMessage
import protocols.IndexatorActorProtocol.StartIndex
import protocols.RankActorProtocol.BuildRankModel
import scala.concurrent.Future
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

@Singleton
class ApplicationActorsService @Inject()(appLifecycle: ApplicationLifecycle, system: ActorSystem) {
//
  private val crawlQueueCreatorActor = system.actorOf(Props(new CrawlTasksCreatorActor))
  private val queueCreatingSheduler = system.scheduler.schedule(0.minute, 3.minute, crawlQueueCreatorActor, new StartMessage())

  private val indexatorActor = system.actorOf(Props(new IndexatorActor()))
  private val indexatorSheduler = system.scheduler.schedule(0.minute, 30.minute, indexatorActor, new StartIndex())

  private val loggerActor = system.actorOf(Props(new LoggerActor()), "logger")

//  private val rankActor = system.actorOf(Props(new RankActor()), "rank")
//  private val rankSheduler = system.scheduler.schedule(0.minute, 120.minute, rankActor, new BuildRankModel())


  appLifecycle.addStopHook { () =>
//    system stop crawlQueueCreatorActor
    Future.successful(())
  }
}
