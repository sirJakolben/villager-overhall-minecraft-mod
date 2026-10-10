package com.smartgolem.client;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

/**
 * The "2_block_climb" animation of the Blockbench project, 4.0F seconds, once. GENERATED from the bbmodel by a script - do not edit by hand.
 * Blockbench shows y up and x mirrored, the model here y down: positions are (-x, y, z) through posVec (which flips y), rotations (-x, -y, z).
 * Bezier keyframes are catmull-rom here (Vanilla has linear and catmull-rom only); a step is a jump of 1 ms.
 */
public final class ClimbGolemAnimations {

    public static final float LENGTH_SECONDS = 4.0F;

    public static final AnimationDefinition CLIMB = AnimationDefinition.Builder.withLength(LENGTH_SECONDS)
                .addAnimation("climb_upper_body", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                        new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                        new Keyframe(0.58333F, KeyframeAnimations.degreeVec(-5.0F, 7.5F, 8.0F), AnimationChannel.Interpolations.CATMULLROM),
                        new Keyframe(0.75F, KeyframeAnimations.degreeVec(-5.0F, 7.5F, 8.0F), AnimationChannel.Interpolations.CATMULLROM),
                        new Keyframe(1.41667F, KeyframeAnimations.degreeVec(-2.5F, -7.0F, -7.0F), AnimationChannel.Interpolations.CATMULLROM),
                        new Keyframe(1.66667F, KeyframeAnimations.degreeVec(-1.0F, -5.0F, -6.0F), AnimationChannel.Interpolations.CATMULLROM),
                        new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                        new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
                .addAnimation("climb_right_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                        new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                        new Keyframe(0.29167F, KeyframeAnimations.degreeVec(-32.0275F, 24.0234F, 19.8594F), AnimationChannel.Interpolations.LINEAR),
                        new Keyframe(0.58333F, KeyframeAnimations.degreeVec(-93.469F, 16.3463F, 6.3361F), AnimationChannel.Interpolations.LINEAR),
                        new Keyframe(0.75F, KeyframeAnimations.degreeVec(-85.8362F, 3.8683F, 7.0882F), AnimationChannel.Interpolations.LINEAR),
                        new Keyframe(1.29167F, KeyframeAnimations.degreeVec(-91.93F, 3.87F, 7.09F), AnimationChannel.Interpolations.LINEAR),
                        new Keyframe(2.0F, KeyframeAnimations.degreeVec(-91.93F, 3.87F, 7.09F), AnimationChannel.Interpolations.LINEAR),
                        new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
                .addAnimation("climb_left_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                        new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                        new Keyframe(0.75F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                        new Keyframe(1.08333F, KeyframeAnimations.degreeVec(-32.0275F, -24.0234F, -19.8594F), AnimationChannel.Interpolations.LINEAR),
                        new Keyframe(1.41667F, KeyframeAnimations.degreeVec(-93.469F, -16.3463F, -6.3361F), AnimationChannel.Interpolations.LINEAR),
                        new Keyframe(1.58333F, KeyframeAnimations.degreeVec(-87.6615F, -2.7244F, -1.056F), AnimationChannel.Interpolations.LINEAR),
                        new Keyframe(1.625F, KeyframeAnimations.degreeVec(-92.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                        new Keyframe(1.91667F, KeyframeAnimations.degreeVec(-95.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                        new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
                .addAnimation("climb_left_leg", new AnimationChannel(AnimationChannel.Targets.POSITION,
                        new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                        new Keyframe(0.70833F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
                .addAnimation("climb_iron_golem", new AnimationChannel(AnimationChannel.Targets.POSITION,
                        new Keyframe(2.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                        new Keyframe(4.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
                .addAnimation("climb_iron_golem", new AnimationChannel(AnimationChannel.Targets.SCALE,
                        new Keyframe(0.0F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
                        new Keyframe(1.999F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
                        new Keyframe(2.0F, KeyframeAnimations.scaleVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
                .addAnimation("climb_iron_golem2", new AnimationChannel(AnimationChannel.Targets.SCALE,
                        new Keyframe(0.0F, KeyframeAnimations.scaleVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                        new Keyframe(1.999F, KeyframeAnimations.scaleVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                        new Keyframe(2.0F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR)))
                .addAnimation("climb_upper_body2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                        new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                        new Keyframe(3.33333F, KeyframeAnimations.degreeVec(-47.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                        new Keyframe(4.0F, KeyframeAnimations.degreeVec(-90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
                .addAnimation("climb_head2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                        new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                        new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                        new Keyframe(3.33333F, KeyframeAnimations.degreeVec(17.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                        new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
                .addAnimation("climb_lower_body2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                        new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                        new Keyframe(3.33333F, KeyframeAnimations.degreeVec(-40.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                        new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
                .addAnimation("climb_arms", new AnimationChannel(AnimationChannel.Targets.POSITION,
                        new Keyframe(3.33333F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                        new Keyframe(4.0F, KeyframeAnimations.posVec(0.0F, 5.0F, 7.0F), AnimationChannel.Interpolations.LINEAR)))
                .addAnimation("climb_arms", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                        new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                        new Keyframe(3.33333F, KeyframeAnimations.degreeVec(89.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
                .addAnimation("climb_legs", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                        new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                        new Keyframe(3.125F, KeyframeAnimations.degreeVec(-4.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                        new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
            .build();

    private ClimbGolemAnimations() {
    }
}