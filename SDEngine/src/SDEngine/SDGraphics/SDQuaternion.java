package SDEngine.SDGraphics;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Vector3f;

import SDEngine.SDGlobal;
import SDEngine.SDLog;
import SDEngine.SDGraphics.SDGraphicsObject;

public class SDQuaternion extends SDGraphicsObject {
  public float x, y, z, w;

  public SDQuaternion(float w, float x, float y, float z) {
    this.w = w;
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public SDQuaternion(Vector3f source) {
    this(0, source.x, source.y, source.z);
  }

  public SDQuaternion() {
    this(0, 0, 0, 0);
  }

  public static double magnitude(SDQuaternion q) {
    return Math.sqrt(q.w * q.w + q.x * q.x + q.y * q.y + q.z * q.z);
  }

  public static void normalize(SDQuaternion q) {
    float m = (float) magnitude(q);
    if (m == 0) {
      SDLog.err("divide by 0");
      return;
    }
    q.w /= m;
    q.x /= m;
    q.y /= m;
    q.z /= m;
  }

  public void normalize() {
    normalize(this);
  }

  public static void mul(SDQuaternion dest, SDQuaternion left,
      SDQuaternion right) {
    if (left.w == 0)
      dest.w = right.w;
    else
      dest.w = left.w * right.w - left.x * right.x - left.y * right.y - left.z
          * right.z;

    if (left.x == 0)
      dest.x = right.x;
    else
      dest.x = left.w * right.x + left.x * right.w + left.y * right.z - left.z
          * right.y;

    if (left.y == 0)
      dest.y = right.y;
    else
      dest.y = left.w * right.y - left.x * right.z + left.y * right.w + left.z
          * right.x;

    if (left.z == 0)
      dest.z = right.z;
    else
      dest.z = left.w * right.z + left.x * right.y - left.y * right.x + left.z
          * right.w;
  }

  public void mul(SDQuaternion right) {
    SDQuaternion left = new SDQuaternion(this.w, this.x, this.y, this.z);
    mul(this, left, right);
  }

  public void rotateY(float angle) {
    rotate(angle, 0, 1, 0);
  }

  public void rotateX(float angle) {
    rotate(angle, 1, 0, 0);
  }

  public void rotateZ(float angle) {
    rotate(angle, 0, 0, 1);
  }

  public void setYRotation(float angle) {
    y = 0;
    rotateY(angle);
  }

  public void setXRotation(float angle) {
    x = 0;
    rotateX(angle);
  }

  public void setZRotation(float angle) {
    z = 0;
    rotateZ(angle);
  }

  public void rotate(float angle, float x, float y, float z) {
    SDQuaternion local = new SDQuaternion();
    float sinVal = (float) Math.sin(angle * SDGlobal.DEG_TO_RAD / 2);
    local.w = (float) Math.cos(angle * SDGlobal.DEG_TO_RAD / 2);
    local.x = sinVal * x;
    local.y = sinVal * y;
    local.z = sinVal * z;

    mul(local);
  }

  public void rotate(float angle, Vector3f axis) {
    rotate(angle, axis.x, axis.y, axis.z);
  }

  public static SDQuaternion conjugate(SDQuaternion q) {
    return new SDQuaternion(q.w, -q.x, -q.y, -q.z);
  }

  public static FloatBuffer matrixFromQuaternion(SDQuaternion q) {
    FloatBuffer matrix = BufferUtils.createFloatBuffer(16);
    float x2 = q.x * q.x, y2 = q.y * q.y, z2 = q.z * q.z;
    matrix.put(new float[] { (1 - 2 * (z2 + y2)),
        (2 * (q.x * q.y + q.w * q.z)), (2 * (q.x * q.z - q.w * q.y)), 0,
        (2 * (q.x * q.y + q.w * q.z)), (1 - 2 * (z2 + x2)),
        (2 * (q.y * q.z + q.w * q.x)), 0, (2 * (q.x * q.z + q.w * q.y)),
        (2 * (q.y * q.z - q.w * q.x)), (1 - 2 * (y2 + x2)), 0, 0, 0, 0, 1 });
    matrix.flip();
    return matrix;
  }

}
