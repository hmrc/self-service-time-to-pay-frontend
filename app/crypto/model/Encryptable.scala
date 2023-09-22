
package crypto.model

trait Encryptable[A] {
  def encrypt: Encrypted[A]
}
