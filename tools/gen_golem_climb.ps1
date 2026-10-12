$ErrorActionPreference = 'Stop'
[Threading.Thread]::CurrentThread.CurrentCulture = [Globalization.CultureInfo]::InvariantCulture
$bb = 'C:\Users\(02) CodingProjekte\Mods\Improved Villagers\assets\Block References\iron_golem\iron_golem.bbmodel'
$outDir = 'C:\Users\(02) CodingProjekte\Mods\Improved Villagers\smart-golem\src\bake\java\com\smartgolem\client'
New-Item -ItemType Directory -Force $outDir | Out-Null
$o = Get-Content -Raw $bb | ConvertFrom-Json

function F([double]$v) { $s = ([Math]::Round($v, 5)).ToString('0.#####'); if ($s -eq '-0') { $s = '0' }; if ($s -notmatch '\.') { $s += '.0' }; return "${s}F" }
function Pname([string]$n) { return 'climb_' + $n.Replace(' ', '_').ToLower() }

$elements = @{}; foreach ($e in $o.elements) { $elements[$e.uuid] = $e }
$groups = @{}; foreach ($g in $o.groups) { $groups[$g.uuid] = $g }

# Blockbench (y up, x mirrored) -> Minecraft model space (y down, feet at y 24)
function McPivot($origin) { return @((-[double]$origin[0]), (24.0 - [double]$origin[1]), [double]$origin[2]) }

$sb = New-Object Text.StringBuilder
function Emit([string]$line) { [void]$script:sb.AppendLine($line) }

function Cubes($part, $pivotMc, $indent) {
    # returns the CubeListBuilder expression for all plain cubes of this group
}

function WalkGroup($node, $parentVar, $parentPivot, $indent) {
    $g = $groups[$node.uuid]
    $name = Pname $g.name
    $pivot = McPivot $g.origin
    $off = @(($pivot[0] - $parentPivot[0]), ($pivot[1] - $parentPivot[1]), ($pivot[2] - $parentPivot[2]))
    $var = ($name -replace '[^a-zA-Z0-9]', '_')
    $plain = @(); $rotated = @(); $kids = @()
    foreach ($c in $node.children) {
        if ($c -is [string]) {
            $e = $elements[$c]
            if ($e.rotation -and (($e.rotation | Where-Object { [double]$_ -ne 0 }).Count -gt 0)) { $rotated += $e } else { $plain += $e }
        } else { $kids += $c }
    }
    $cubeExpr = 'CubeListBuilder.create()'
    foreach ($e in $plain) {
        $uv = if ($e.uv_offset) { $e.uv_offset } else { @(0, 0) }
        $minx = -[double]$e.to[0] - $pivot[0]; $miny = 24.0 - [double]$e.to[1] - $pivot[1]; $minz = [double]$e.from[2] - $pivot[2]
        $sx = [double]$e.to[0] - [double]$e.from[0]; $sy = [double]$e.to[1] - [double]$e.from[1]; $sz = [double]$e.to[2] - [double]$e.from[2]
        $mir = [bool]$e.mirror_uv
        $def = ''
        if ($e.inflate -and [double]$e.inflate -ne 0) { $def = ', new CubeDeformation(' + (F $e.inflate) + ')' }
        $cubeExpr += "`n$indent        .texOffs($([int]$uv[0]), $([int]$uv[1]))"
        if ($mir) { $cubeExpr += '.mirror(true)' }
        $cubeExpr += ".addBox($(F $minx), $(F $miny), $(F $minz), $(F $sx), $(F $sy), $(F $sz)$def)"
        if ($mir) { $cubeExpr += '.mirror(false)' }
    }
    Emit "$indent`PartDefinition $var = $parentVar.addOrReplaceChild(`"$name`", $cubeExpr,"
    Emit "$indent        PartPose.offset($(F $off[0]), $(F $off[1]), $(F $off[2])));"
    foreach ($e in $rotated) {
        $cp = McPivot $e.origin
        $rel = @(($cp[0] - $pivot[0]), ($cp[1] - $pivot[1]), ($cp[2] - $pivot[2]))
        $uv = if ($e.uv_offset) { $e.uv_offset } else { @(0, 0) }
        $minx = -[double]$e.to[0] - $cp[0]; $miny = 24.0 - [double]$e.to[1] - $cp[1]; $minz = [double]$e.from[2] - $cp[2]
        $sx = [double]$e.to[0] - [double]$e.from[0]; $sy = [double]$e.to[1] - [double]$e.from[1]; $sz = [double]$e.to[2] - [double]$e.from[2]
        $rx = [Math]::PI / 180.0 * (-[double]$e.rotation[0]); $ry = [Math]::PI / 180.0 * (-[double]$e.rotation[1]); $rz = [Math]::PI / 180.0 * ([double]$e.rotation[2])
        $mir = [bool]$e.mirror_uv
        $def = ''
        if ($e.inflate -and [double]$e.inflate -ne 0) { $def = ', new CubeDeformation(' + (F $e.inflate) + ')' }
        $cname = $name + '_' + $e.name
        $cexpr = "CubeListBuilder.create().texOffs($([int]$uv[0]), $([int]$uv[1]))"
        if ($mir) { $cexpr += '.mirror(true)' }
        $cexpr += ".addBox($(F $minx), $(F $miny), $(F $minz), $(F $sx), $(F $sy), $(F $sz)$def)"
        Emit "$indent$var.addOrReplaceChild(`"$cname`","
        Emit "$indent        $cexpr,"
        Emit "$indent        PartPose.offsetAndRotation($(F $rel[0]), $(F $rel[1]), $(F $rel[2]), $(F $rx), $(F $ry), $(F $rz)));"
    }
    foreach ($k in $kids) { WalkGroup $k $var $pivot $indent }
}

Emit '        PartDefinition climb = root.addOrReplaceChild("climb", CubeListBuilder.create(), PartPose.ZERO);'
foreach ($top in $o.outliner) { WalkGroup $top 'climb' @(0.0, 0.0, 0.0) '        ' }
$partsBody = $sb.ToString()

$parts = @"
package com.smartgolem.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The parts of the two-block climb (assets/Block References/iron_golem/iron_golem.bbmodel, Blockbench "Modded Entity"): two golems, "Iron golem" (standing)
 * and "Iron_golem2" (the pose that hangs on the ledge), one hidden while the other shows - ClimbGolemAnimations swaps them with scale keyframes.
 * GENERATED from the bbmodel by a script - do not edit by hand; export the model again instead. Every part is named climb_<group name>.
 */
final class ClimbGolemParts {

    private ClimbGolemParts() {
    }

    static void addTo(PartDefinition root) {
$partsBody    }
}
"@
[IO.File]::WriteAllText((Join-Path $outDir 'ClimbGolemParts.java'), $parts, (New-Object Text.UTF8Encoding($false)))

# ---- animation ----
$anim = $o.animations[0]
$ab = New-Object Text.StringBuilder
$length = [double]$anim.length
foreach ($prop in $anim.animators.PSObject.Properties) {
    $an = $prop.Value
    if (-not $an.keyframes -or $an.keyframes.Count -eq 0) { continue }
    $bone = Pname $an.name
    foreach ($channel in 'position', 'rotation', 'scale') {
        $ks = @($an.keyframes | Where-Object { $_.channel -eq $channel } | Sort-Object { [double]$_.time })
        if ($ks.Count -eq 0) { continue }
        $target = @{ position = 'POSITION'; rotation = 'ROTATION'; scale = 'SCALE' }[$channel]
        $lines = @()
        $prev = $null
        foreach ($k in $ks) {
            $x = [double]$k.data_points[0].x; $y = [double]$k.data_points[0].y; $z = [double]$k.data_points[0].z
            switch ($channel) {
                'position' { $vec = "KeyframeAnimations.posVec($(F (-$x)), $(F $y), $(F $z))" }
                'rotation' { $vec = "KeyframeAnimations.degreeVec($(F (-$x)), $(F (-$y)), $(F $z))" }
                'scale' { $vec = "KeyframeAnimations.scaleVec($(F $x), $(F $y), $(F $z))" }
            }
            $t = [double]$k.time
            $interp = switch ($k.interpolation) { 'linear' { 'LINEAR' } 'step' { 'LINEAR' } default { 'CATMULLROM' } }
            if ($k.interpolation -eq 'step' -and $prev -ne $null) {
                # a step holds the value before it until the keyframe's time: the previous value just before, then the jump
                $lines += "new Keyframe($(F ($t - 0.001)), $($prev.vec), AnimationChannel.Interpolations.LINEAR)"
            }
            $lines += "new Keyframe($(F $t), $vec, AnimationChannel.Interpolations.$interp)"
            $prev = @{ vec = $vec }
        }
        [void]$ab.AppendLine("                .addAnimation(`"$bone`", new AnimationChannel(AnimationChannel.Targets.$target,")
        [void]$ab.AppendLine('                        ' + ($lines -join ",`n                        ") + '))')
    }
}
$animCode = @"
package com.smartgolem.client;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

/**
 * The "$($anim.name)" animation of the Blockbench project, $(F $length) seconds, once. GENERATED from the bbmodel by a script - do not edit by hand.
 * Blockbench shows y up and x mirrored, the model here y down: positions are (-x, y, z) through posVec (which flips y), rotations (-x, -y, z).
 * Bezier keyframes are catmull-rom here (Vanilla has linear and catmull-rom only); a step is a jump of 1 ms.
 */
public final class ClimbGolemAnimations {

    public static final float LENGTH_SECONDS = $(F $length);

    public static final AnimationDefinition CLIMB = AnimationDefinition.Builder.withLength(LENGTH_SECONDS)
$($ab.ToString().TrimEnd())
            .build();

    private ClimbGolemAnimations() {
    }
}
"@
# The animation is baked by ./gradlew :smart-golem:bakeClimb (Blockbench's exact curves) - the generated ClimbGolemAnimations are no longer used.
Get-ChildItem $outDir | Select Name, Length
