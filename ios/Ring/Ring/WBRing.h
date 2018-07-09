//
//  WBRing.h
//  WBRing
//
//  Created by chris on 6/28/18.
//  Copyright © 2018 Warebots, LLC. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>
#import "WBNode.h"

@interface WBRing : RCTEventEmitter <RCTBridgeModule>
@property WBNode *node;
@property dispatch_queue_t queue;
@end
