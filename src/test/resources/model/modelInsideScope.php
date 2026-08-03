<?php

namespace App {
class User extends \Hyperf\Database\Model\Model
{
    public function scopeFirstId(\Hyperf\Database\Model\Builder $query)
    {
        return $query->where('<caret>');
    }
}
}
