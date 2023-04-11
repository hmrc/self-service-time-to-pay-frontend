import sbt.Compile
import sbt.Keys.compile
import wartremover.Wart._
import wartremover.WartRemover.autoImport.{wartremoverErrors, wartremoverWarnings}

object  WartRemoverSettings {

  lazy val wartRemoverWarning = {
    val warningWarts = Seq(
      JavaSerializable,
      AsInstanceOf
    )
    Compile / compile / wartremoverWarnings ++= warningWarts
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
      TryPartial,
      Var,
      While
    )

    Compile / compile / wartremoverErrors ++= errorWarts
  }
}