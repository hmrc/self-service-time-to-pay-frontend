
package crypto.model

trait Encrypted[A] {
  def decrypt: A
}
