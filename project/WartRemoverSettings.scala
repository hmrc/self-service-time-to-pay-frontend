import sbt.Compile
import sbt.Keys.compile
import wartremover.Wart._
import wartremover.{wartremoverErrors, wartremoverWarnings}

object  WartRemoverSettings {

  lazy val wartRemoverWarning = {
    val warningWarts = Seq(
      JavaSerializable,
      AsInstanceOf
    )
    wartremoverWarnings in(Compile, compile) ++= warningWarts
  }
  lazy val wartRemoverError = {
    val errorWarts = Seq(
      ArrayEquals,
      AnyVal,
      EitherProjectionPartial,
      Enumeration,
      ExplicitImplicitTypes,
      FinalVal,
      JavaConversions,
      JavaSerializable,
      LeakingSealed,
      MutableDataStructures,
      Null,
      OptionPartial,
      Recursion,
      Return,
      TraversableOps,
      TryPartial,
      Var,
      While
    )

    wartremoverErrors in(Compile, compile) ++= errorWarts
  }
}