package com.smartgolem.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Dev tool (./gradlew :smart-golem:bakeClimb), not part of the mod: turns the climb animation of the Blockbench project into one skeleton.
 *
 * The project has two golems (a standing one, "Iron golem", and the one that hangs on the ledge, "Iron_golem2") swapped at 2.0 s. This tool
 * 1. evaluates the keyframes exactly like Blockbench does (linear, catmull-rom, BEZIER with the handles, step - ported from Blockbench's own code, so the
 *    curves are the ones drawn in Blockbench, not an approximation);
 * 2. poses the existing two-golem model (ClimbGolemParts) at every tick with Vanilla's own ModelPart math;
 * 3. for each of the golem's 7 parts takes the pose of its cube in whichever golem is shown at that time, as a rigid motion away from the standing
 *    rest pose - one skeleton, same cubes, same texture;
 * 4. takes the golem's movement out of the parts (it is done by the entity: still until 2 s, then a straight line up and forward) and writes
 *    ClimbPose.java with the tables.
 * It prints how well the tables follow the exact curves, how well the two golems agree at the swap and how well the end matches the standing pose.
 */
public final class BakeClimb {

    /** The parts of the baked golem, in the order of the tables. */
    enum Part {
        UPPER_BODY, HEAD, LOWER_BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG
    }

    private static final int FPS = 48;
    private static final double SWAP_TIME = 2.0;
    /** The entity moves between these two times. */
    private static final double MOVE_START = 2.0;
    private static final double MOVE_END = 4.0;

    // ---------------------------------------------------------------- Blockbench keyframes

    record Keyframe(double time, String interpolation, double[] value, double[] leftTime, double[] leftValue, double[] rightTime, double[] rightValue) {
    }

    /** The keyframes of one group, by channel (position, rotation, scale), sorted by time. */
    record Animator(String name, Map<String, List<Keyframe>> channels) {
    }

    private static double[] vector(JsonObject object, String key, double fallback) {
        double[] result = {fallback, fallback, fallback};
        if (object.has(key) && object.get(key).isJsonArray()) {
            JsonArray array = object.getAsJsonArray(key);
            for (int i = 0; i < 3 && i < array.size(); i++) {
                result[i] = array.get(i).getAsDouble();
            }
        }
        return result;
    }

    private static List<Animator> readAnimators(JsonObject bbmodel) {
        JsonObject animation = bbmodel.getAsJsonArray("animations").get(0).getAsJsonObject();
        List<Animator> animators = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : animation.getAsJsonObject("animators").entrySet()) {
            JsonObject animator = entry.getValue().getAsJsonObject();
            if (!animator.has("keyframes")) {
                continue;
            }
            Map<String, List<Keyframe>> channels = new HashMap<>();
            for (JsonElement element : animator.getAsJsonArray("keyframes")) {
                JsonObject keyframe = element.getAsJsonObject();
                JsonObject point = keyframe.getAsJsonArray("data_points").get(0).getAsJsonObject();
                double[] value = {point.get("x").getAsDouble(), point.get("y").getAsDouble(), point.get("z").getAsDouble()};
                channels.computeIfAbsent(keyframe.get("channel").getAsString(), k -> new ArrayList<>()).add(new Keyframe(
                        keyframe.get("time").getAsDouble(), keyframe.get("interpolation").getAsString(), value,
                        vector(keyframe, "bezier_left_time", -0.1), vector(keyframe, "bezier_left_value", 0.0),
                        vector(keyframe, "bezier_right_time", 0.1), vector(keyframe, "bezier_right_value", 0.0)));
            }
            channels.values().forEach(list -> list.sort(Comparator.comparingDouble(Keyframe::time)));
            animators.add(new Animator(animator.get("name").getAsString(), channels));
        }
        return animators;
    }

    /** Blockbench's Animator.interpolate for one axis: the value of the channel at the time. */
    static double evaluate(List<Keyframe> keyframes, double time, int axis) {
        Keyframe before = null;
        Keyframe after = null;
        double beforeTime = 0;
        double afterTime = 0;
        for (Keyframe keyframe : keyframes) {
            double t = keyframe.time();
            if (t < time) {
                if (before == null || t > beforeTime) {
                    before = keyframe;
                    beforeTime = t;
                }
            } else if (after == null || t < afterTime) {
                after = keyframe;
                afterTime = t;
            }
        }
        double epsilon = 1.0 / 1200.0;
        Keyframe chosen = null;
        if (before != null && Math.abs(beforeTime - time) < epsilon) {
            chosen = before;
        } else if (after != null && Math.abs(afterTime - time) < epsilon) {
            chosen = after;
        } else if (before != null && before.interpolation().equals("step")) {
            chosen = before;
        } else if (before != null && after == null) {
            chosen = before;
        } else if (after != null && before == null) {
            chosen = after;
        } else if (before != null) {
            double alpha = (time - beforeTime) / (afterTime - beforeTime);
            String a = before.interpolation();
            String b = after.interpolation();
            if (a.equals("linear") && (b.equals("linear") || b.equals("step"))) {
                return before.value()[axis] + (after.value()[axis] - before.value()[axis]) * alpha;
            } else if (a.equals("catmullrom") || b.equals("catmullrom")) {
                int index = keyframes.indexOf(before);
                Keyframe previous = index > 0 ? keyframes.get(index - 1) : null;
                Keyframe next = index + 2 < keyframes.size() ? keyframes.get(index + 2) : null;
                return catmullRom(previous, before, after, next, axis, alpha);
            } else if (a.equals("bezier") || b.equals("bezier")) {
                return bezier(before, after, axis, alpha);
            }
        }
        if (chosen == null) {
            throw new IllegalStateException("no keyframe for " + time);
        }
        return chosen.value()[axis];
    }

    /** Blockbench's getCatmullromLerp: a three.js SplineCurve through (time, value) of the neighbouring keyframes, evaluated at index position. */
    private static double catmullRom(Keyframe previous, Keyframe before, Keyframe after, Keyframe next, int axis, double alpha) {
        List<double[]> points = new ArrayList<>();
        if (previous != null) {
            points.add(new double[] {previous.time(), previous.value()[axis]});
        }
        points.add(new double[] {before.time(), before.value()[axis]});
        points.add(new double[] {after.time(), after.value()[axis]});
        if (next != null) {
            points.add(new double[] {next.time(), next.value()[axis]});
        }
        double c = (alpha + (previous != null ? 1 : 0)) / (points.size() - 1);
        int last = points.size() - 1;
        double p = last * c;
        int intPoint = (int) Math.floor(p);
        double weight = p - intPoint;
        if (weight == 0 && intPoint == last) {
            intPoint = last - 1;
            weight = 1;
        }
        double[] p0 = points.get(intPoint == 0 ? intPoint : intPoint - 1);
        double[] p1 = points.get(intPoint);
        double[] p2 = points.get(intPoint > last - 1 ? last : intPoint + 1);
        double[] p3 = points.get(intPoint > last - 2 ? last : intPoint + 2);
        return catmullRomValue(weight, p0[1], p1[1], p2[1], p3[1]);
    }

    private static double catmullRomValue(double t, double p0, double p1, double p2, double p3) {
        double v0 = (p2 - p0) * 0.5;
        double v1 = (p3 - p1) * 0.5;
        double t2 = t * t;
        double t3 = t * t2;
        return (2 * p1 - 2 * p2 + v0 + v1) * t3 + (-3 * p1 + 3 * p2 - 2 * v0 - v1) * t2 + v0 * t + p1;
    }

    /** Blockbench's getBezierLerp: the cubic curve through the two keyframes with their handles, looked up by time (201 samples, interpolated between the two nearest). */
    private static double bezier(Keyframe before, Keyframe after, int axis, double alpha) {
        double startValue = before.value()[axis];
        double endValue = after.value()[axis];
        double length = after.time() - before.time();
        double rightTime = Math.min(Math.max(before.rightTime()[axis], 0), length);
        double leftTime = Math.min(Math.max(after.leftTime()[axis], -length), 0);
        double[] x = {before.time(), before.time() + rightTime, after.time() + leftTime, after.time()};
        double[] y = {startValue, startValue + before.rightValue()[axis], endValue + after.leftValue()[axis], endValue};
        double target = before.time() + (after.time() - before.time()) * alpha;
        int divisions = 200;
        double[][] samples = new double[divisions + 1][];
        for (int i = 0; i <= divisions; i++) {
            double t = (double) i / divisions;
            samples[i] = new double[] {cubic(t, x), cubic(t, y)};
        }
        double[] nearest = null;
        double best = Double.POSITIVE_INFINITY;
        for (double[] sample : samples) {
            double d = Math.abs(sample[0] - target);
            if (d < best) {
                best = d;
                nearest = sample;
            }
        }
        double[] second = null;
        best = Double.POSITIVE_INFINITY;
        for (double[] sample : samples) {
            if (sample == nearest) {
                continue;
            }
            double d = Math.abs(sample[0] - target);
            if (d < best) {
                best = d;
                second = sample;
            }
        }
        double lerp = Math.min(Math.max((target - nearest[0]) / (second[0] - nearest[0]), 0), 1);
        return nearest[1] + (second[1] - nearest[1]) * lerp;
    }

    private static double cubic(double t, double[] p) {
        double k = 1 - t;
        return p[0] * k * k * k + 3 * p[1] * k * k * t + 3 * p[2] * k * t * t + p[3] * t * t * t;
    }

    // ---------------------------------------------------------------- the two-golem model, posed

    /** A part name the way ClimbGolemParts has it: climb_<group name, lower case, spaces as underscores>. */
    private static String partName(String group) {
        return "climb_" + group.replace(' ', '_').toLowerCase(Locale.ROOT);
    }

    private final List<Animator> animators;
    private final ModelPart root;

    private BakeClimb(List<Animator> animators) {
        this.animators = animators;
        MeshDefinition mesh = new MeshDefinition();
        ClimbGolemParts.addTo(mesh.getRoot());
        this.root = LayerDefinition.create(mesh, 128, 128).bakeRoot();
    }

    /** Poses the model like Blockbench's animation preview at the time (the groups' own rotation and position keyframes; scale is the swap and is not used here). */
    private void pose(double time) {
        for (ModelPart part : root.getAllParts()) {
            part.resetPose();
        }
        var lookup = root.createPartLookup();
        for (Animator animator : animators) {
            ModelPart part = lookup.apply(partName(animator.name()));
            if (part == null) {
                throw new IllegalStateException("no part for group " + animator.name());
            }
            List<Keyframe> position = animator.channels().get("position");
            if (position != null) {
                // Blockbench shows y up and x mirrored; the model has y down: (-x, -y, z), the same as the generated animation.
                part.offsetPos(new Vector3f((float) -evaluate(position, time, 0), (float) -evaluate(position, time, 1), (float) evaluate(position, time, 2)));
            }
            List<Keyframe> rotation = animator.channels().get("rotation");
            if (rotation != null) {
                part.offsetRotation(new Vector3f((float) Math.toRadians(-evaluate(rotation, time, 0)), (float) Math.toRadians(-evaluate(rotation, time, 1)),
                        (float) Math.toRadians(evaluate(rotation, time, 2))));
            }
        }
    }

    /** The poses of the cubes in model space (blocks): the frame at each cube's middle, and the middle in the part's own frame (pixels). */
    private record CubeFrame(Matrix4f frame, Vector3f centerInPart) {
    }

    private static Part canonical(String path, int cubeIndex, int golem) {
        String last = path.substring(path.lastIndexOf('/') + 1);
        if (golem == 1) {
            return switch (last) {
                case "climb_upper_body" -> Part.UPPER_BODY;
                case "climb_head" -> cubeIndex == 0 ? Part.HEAD : null;
                case "climb_lower_body" -> Part.LOWER_BODY;
                case "climb_right_arm" -> Part.RIGHT_ARM;
                case "climb_left_arm" -> Part.LEFT_ARM;
                case "climb_right_leg" -> Part.RIGHT_LEG;
                case "climb_left_leg" -> Part.LEFT_LEG;
                default -> null;
            };
        }
        return switch (last) {
            case "climb_upper_body2" -> Part.UPPER_BODY;
            case "climb_head2" -> cubeIndex == 0 ? Part.HEAD : null;
            case "climb_lower_body2" -> Part.LOWER_BODY;
            case "climb_arms_right_arm" -> Part.RIGHT_ARM;
            case "climb_arms_left_arm" -> Part.LEFT_ARM;
            case "climb_legs" -> cubeIndex == 0 ? Part.RIGHT_LEG : Part.LEFT_LEG;
            default -> null;
        };
    }

    private Map<Part, CubeFrame> frames(double time, int golem) {
        pose(time);
        Map<Part, CubeFrame> result = new EnumMap<>(Part.class);
        String prefix = golem == 1 ? "/climb/climb_iron_golem/" : "/climb/climb_iron_golem2/";
        root.visit(new PoseStack(), (pose, path, cubeIndex, cube) -> {
            if (!(path + "/").startsWith(prefix)) {
                return;
            }
            Part part = canonical(path, cubeIndex, golem);
            if (part == null) {
                return;
            }
            Vector3f center = new Vector3f((cube.minX + cube.maxX) / 2, (cube.minY + cube.maxY) / 2, (cube.minZ + cube.maxZ) / 2);
            Matrix4f frame = new Matrix4f(pose.pose()).translate(center.x / 16, center.y / 16, center.z / 16);
            result.put(part, new CubeFrame(frame, center));
        });
        if (result.size() != Part.values().length) {
            throw new IllegalStateException("found " + result.keySet() + " in golem " + golem);
        }
        return result;
    }

    // ---------------------------------------------------------------- the baked skeleton

    /** A part's pose: where its pivot is (pixels, model space) and how it is turned (ZYX Euler radians, like ModelPart). */
    record Pose(float x, float y, float z, float xRot, float yRot, float zRot, Matrix4f matrix) {
    }

    private Map<Part, Matrix4f> restFrames;
    private Map<Part, Vector3f> centers;

    /** The pivot frame of each part at the time: its cube's frame in the golem shown then, moved back to the pivot (the cube middle in golem 1's part frame). */
    private Map<Part, Matrix4f> pivotFrames(double time, int golem) {
        Map<Part, CubeFrame> frames = frames(time, golem);
        Map<Part, Matrix4f> result = new EnumMap<>(Part.class);
        for (Part part : Part.values()) {
            Vector3f c = centers.get(part);
            result.put(part, new Matrix4f(frames.get(part).frame()).translate(-c.x / 16, -c.y / 16, -c.z / 16));
        }
        return result;
    }

    private static Vector3f translation(Matrix4f matrix) {
        return matrix.getTranslation(new Vector3f()).mul(16.0F);
    }

    private static Vector3f euler(Matrix4f matrix) {
        return matrix.get3x3(new Matrix3f()).getEulerAnglesZYX(new Vector3f());
    }

    /** The angle of the rotation that takes a to b (degrees). */
    private static double angleBetween(Matrix4f a, Matrix4f b) {
        Quaternionf qa = a.getUnnormalizedRotation(new Quaternionf()).normalize();
        Quaternionf qb = b.getUnnormalizedRotation(new Quaternionf()).normalize();
        double dot = Math.abs(qa.dot(qb));
        return Math.toDegrees(2 * Math.acos(Math.min(1.0, dot)));
    }

    private double forwardPixels;
    private double upPixels;

    /** How far the entity has moved at the time, 0 to 1: still until MOVE_START, then a straight line. */
    private static double travel(double time) {
        return Math.min(Math.max((time - MOVE_START) / (MOVE_END - MOVE_START), 0.0), 1.0);
    }

    /** The pivot frame of the part, with the entity's movement taken out (model space: forward is -z, up is -y). */
    private Matrix4f local(Matrix4f pivot, double time) {
        double s = travel(time);
        return new Matrix4f().translation(0.0F, (float) (-upPixels * s / 16.0), (float) (-forwardPixels * s / 16.0)).mul(pivot);
    }

    /** The two golems agree at the swap except for the right arm (about 8 degrees): over this long before it, the standing golem is faded into the hanging one. */
    private static final double BLEND_SECONDS = 0.15;
    /** The end of the animation is up to 2 degrees and 0.3 px off the standing pose; over this long it is eased into exactly that, so Vanilla's golem takes over without a jump. */
    private static final double SETTLE_SECONDS = 0.5;

    private static double smooth(double t) {
        double c = Math.min(Math.max(t, 0.0), 1.0);
        return c * c * (3 - 2 * c);
    }

    /** The pivot frames at the time: golem 1 before the swap (faded into golem 2 shortly before it), golem 2 from then on. */
    private Map<Part, Matrix4f> at(double time) {
        if (time >= SWAP_TIME) {
            return pivotFrames(time, 2);
        }
        Map<Part, Matrix4f> one = pivotFrames(time, 1);
        if (time <= SWAP_TIME - BLEND_SECONDS) {
            return one;
        }
        Map<Part, Matrix4f> two = pivotFrames(time, 2);
        float s = (float) smooth((time - (SWAP_TIME - BLEND_SECONDS)) / BLEND_SECONDS);
        Map<Part, Matrix4f> result = new EnumMap<>(Part.class);
        for (Part part : Part.values()) {
            Vector3f p = translation(one.get(part)).lerp(translation(two.get(part)), s);
            Quaternionf q = one.get(part).getUnnormalizedRotation(new Quaternionf()).normalize()
                    .slerp(two.get(part).getUnnormalizedRotation(new Quaternionf()).normalize(), s);
            result.put(part, new Matrix4f().translation(p.x / 16, p.y / 16, p.z / 16).rotate(q));
        }
        return result;
    }

    /** What is added at the end so that the last row is exactly the standing pose: x, y, z, xRot, yRot, zRot per part. */
    private Map<Part, float[]> settle;

    /** The row of a part at the time: pivot (px), ZYX Euler (radians), the entity's movement taken out and the end eased into the standing pose. */
    private float[] six(Part part, double time, Map<Part, Matrix4f> pivots) {
        Matrix4f m = local(pivots.get(part), time);
        Vector3f p = translation(m);
        Vector3f e = euler(m);
        float s = (float) smooth((time - (MOVE_END - SETTLE_SECONDS)) / SETTLE_SECONDS);
        float[] d = settle == null ? new float[6] : settle.get(part);
        return new float[] {p.x + d[0] * s, p.y + d[1] * s, p.z + d[2] * s, e.x + d[3] * s, e.y + d[4] * s, e.z + d[5] * s};
    }

    public static void main(String[] args) throws IOException {
        JsonObject bbmodel = JsonParser.parseString(Files.readString(Path.of(args[0]), StandardCharsets.UTF_8)).getAsJsonObject();
        BakeClimb tool = new BakeClimb(readAnimators(bbmodel));
        tool.run(Path.of(args[1]));
    }

    private void run(Path outDir) throws IOException {
        // The rest pose: golem 1 at 0 s. Its cubes' middles in the part frames are what the pivots are measured from.
        pose(0);
        centers = new EnumMap<>(Part.class);
        Map<Part, CubeFrame> rest = frames(0, 1);
        for (Part part : Part.values()) {
            centers.put(part, rest.get(part).centerInPart());
        }
        restFrames = pivotFrames(0, 1);
        System.out.println("== rest pose (pivots, pixels) ==");
        for (Part part : Part.values()) {
            System.out.printf(Locale.ROOT, "%-12s %s%n", part, fmt(translation(restFrames.get(part))));
        }

        // The swap: both golems at 2.0 s.
        Map<Part, Matrix4f> one = pivotFrames(SWAP_TIME, 1);
        Map<Part, Matrix4f> two = pivotFrames(SWAP_TIME, 2);
        System.out.println("== swap at 2.0 s: golem 1 against golem 2 (position px, angle deg) ==");
        for (Part part : Part.values()) {
            Vector3f d = translation(one.get(part)).sub(translation(two.get(part)));
            System.out.printf(Locale.ROOT, "%-12s %6.2f px  %6.2f deg%n", part, d.length(), angleBetween(one.get(part), two.get(part)));
        }

        // The end: golem 2 at 4.0 s against the rest pose; the entity's movement is what is left.
        Map<Part, Matrix4f> end = pivotFrames(MOVE_END, 2);
        Vector3f upper = translation(end.get(Part.UPPER_BODY)).sub(translation(restFrames.get(Part.UPPER_BODY)));
        upPixels = upper.y;
        forwardPixels = upper.z;
        System.out.printf(Locale.ROOT, "== end at 4.0 s: the upper body is moved by %s px - the entity moves up %.3f px, forward %.3f px (%.4f / %.4f blocks) ==%n",
                fmt(upper), -upPixels, -forwardPixels, -upPixels / 16, -forwardPixels / 16);
        // The entity climbs exactly two blocks.
        upPixels = -32.0;
        settle = null;
        Map<Part, float[]> settling = new EnumMap<>(Part.class);
        for (Part part : Part.values()) {
            float[] last = six(part, MOVE_END, end);
            Vector3f rp = translation(restFrames.get(part));
            float[] d = {rp.x - last[0], rp.y - last[1], rp.z - last[2], -last[3], -last[4], -last[5]};
            settling.put(part, d);
            System.out.printf(Locale.ROOT, "%-12s end is off the standing pose by %6.3f px, turned %s deg - eased away over the last %.1f s%n", part,
                    Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]),
                    fmt(new Vector3f((float) Math.toDegrees(-d[3]), (float) Math.toDegrees(-d[4]), (float) Math.toDegrees(-d[5]))), SETTLE_SECONDS);
        }
        settle = settling;

        // The tables: one row per tick.
        int frames = (int) Math.round(MOVE_END * FPS) + 1;
        float[][][] table = new float[Part.values().length][frames][];
        double maxStep = 0;
        for (int f = 0; f < frames; f++) {
            double time = (double) f / FPS;
            Map<Part, Matrix4f> pivots = at(time);
            for (Part part : Part.values()) {
                float[] row = six(part, time, pivots);
                if (f > 0) {
                    float[] previous = table[part.ordinal()][f - 1];
                    for (int k = 3; k < 6; k++) {
                        row[k] = unwrap(row[k], previous[k]);
                        maxStep = Math.max(maxStep, Math.toDegrees(Math.abs(row[k] - previous[k])));
                    }
                }
                table[part.ordinal()][f] = row;
            }
        }
        System.out.printf(Locale.ROOT, "== table: %d rows at %d per second, biggest turn of one angle between two rows %.2f deg ==%n", frames, FPS, maxStep);

        // How well do the rows (linear between them) follow the exact curves? Checked every millisecond.
        double maxPosition = 0;
        double maxAngle = 0;
        double worstTime = 0;
        Part worstPart = null;
        for (int ms = 0; ms <= (int) (MOVE_END * 1000); ms++) {
            double time = ms / 1000.0;
            Map<Part, Matrix4f> exact = at(time);
            double position = time * FPS;
            int f = Math.min((int) Math.floor(position), frames - 2);
            double alpha = position - f;
            for (Part part : Part.values()) {
                float[] a = table[part.ordinal()][f];
                float[] b = table[part.ordinal()][f + 1];
                float[] x = six(part, time, exact);
                for (int k = 3; k < 6; k++) {
                    x[k] = unwrap(x[k], a[k]);
                }
                double[] q = new double[6];
                for (int k = 0; k < 6; k++) {
                    q[k] = a[k] + (b[k] - a[k]) * alpha;
                }
                double positionError = Math.sqrt(Math.pow(x[0] - q[0], 2) + Math.pow(x[1] - q[1], 2) + Math.pow(x[2] - q[2], 2));
                Matrix4f exactRotation = new Matrix4f().rotationZYX(x[5], x[4], x[3]);
                Matrix4f approxRotation = new Matrix4f().rotationZYX((float) q[5], (float) q[4], (float) q[3]);
                double angleError = angleBetween(exactRotation, approxRotation);
                if (positionError > maxPosition) {
                    maxPosition = positionError;
                    worstTime = time;
                    worstPart = part;
                }
                maxAngle = Math.max(maxAngle, angleError);
            }
        }
        System.out.printf(Locale.ROOT, "== rows against the exact Blockbench curves: at most %.4f px (%s at %.3f s) and %.4f deg off ==%n", maxPosition, worstPart, worstTime, maxAngle);

        writeTables(outDir, table, frames);
    }

    private static float unwrap(float angle, float reference) {
        double twoPi = Math.PI * 2;
        double a = angle;
        while (a - reference > Math.PI) {
            a -= twoPi;
        }
        while (a - reference < -Math.PI) {
            a += twoPi;
        }
        return (float) a;
    }

    private static String fmt(Vector3f v) {
        return String.format(Locale.ROOT, "(%.3f, %.3f, %.3f)", v.x, v.y, v.z);
    }

    private void writeTables(Path outDir, float[][][] table, int frames) throws IOException {
        StringBuilder out = new StringBuilder();
        out.append("package com.smartgolem.climb;\n\n");
        out.append("/**\n");
        out.append(" * The climb animation of the Blockbench project (2_block_climb, 4 seconds) as one skeleton: for each part of the golem its pivot (pixels, model space)\n");
        out.append(" * and its turn (ZYX Euler, radians) per row (48 a second), with the golem's own movement (see ENTITY_*) taken out. GENERATED by ./gradlew :smart-golem:bakeClimb\n");
        out.append(" * from the bbmodel - do not edit by hand. The curves are Blockbench's own, bezier handles included; rows are linear between ticks.\n");
        out.append(" */\n");
        out.append("public final class ClimbPose {\n\n");
        out.append("    public static final int ROWS = ").append(frames).append(";\n");
        out.append("    public static final int ROWS_PER_SECOND = ").append(FPS).append(";\n");
        out.append("    /** The entity stands still until this time (seconds) and then moves in a straight line to the end of the animation. */\n");
        out.append("    public static final float MOVE_START_SECONDS = ").append((float) MOVE_START).append("F;\n");
        out.append("    public static final float MOVE_END_SECONDS = ").append((float) MOVE_END).append("F;\n");
        out.append("    /** How far the entity moves in the end, in pixels of the model: forward (along the way the golem faces) and up. */\n");
        out.append(String.format(Locale.ROOT, "    public static final float ENTITY_FORWARD_PIXELS = %.5fF;%n", -forwardPixels));
        out.append(String.format(Locale.ROOT, "    public static final float ENTITY_UP_PIXELS = %.5fF;%n%n", -upPixels));
        out.append("    /** Per part: x, y, z, xRot, yRot, zRot for each row. */\n");
        for (Part part : Part.values()) {
            out.append("    public static final float[] ").append(part.name()).append(" = {\n");
            for (int f = 0; f < frames; f++) {
                float[] row = table[part.ordinal()][f];
                out.append("        ");
                for (int k = 0; k < 6; k++) {
                    out.append(String.format(Locale.ROOT, "%.5fF,", row[k]));
                    if (k < 5) {
                        out.append(' ');
                    }
                }
                out.append('\n');
            }
            out.append("    };\n\n");
        }
        out.append("    private ClimbPose() {\n    }\n}\n");
        Files.writeString(outDir.resolve("ClimbPose.java"), out.toString(), StandardCharsets.UTF_8);
        System.out.println("wrote " + outDir.resolve("ClimbPose.java"));
    }
}
